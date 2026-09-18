#!/usr/bin/env python3
"""Visual smoke of the authenticated Studio UI with explicitly synthetic API responses."""

from __future__ import annotations

import json
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread
from urllib.parse import urlsplit

from playwright.sync_api import expect, sync_playwright


ROOT = Path(__file__).resolve().parent.parent
STATIC = ROOT / "server/build/resources/main/static/studio"
OUTPUT = ROOT / "output/screenshots/v2-studio-review-fixture"
SAMPLE = ROOT / "docs/v2/contracts/example-apple-story-pack.json"
APPLE_SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="480" height="340" viewBox="0 0 480 340">
<rect width="480" height="340" fill="#fff4b5"/><circle cx="240" cy="181" r="91" fill="#dc3545" stroke="#172033" stroke-width="8"/>
<path d="M238 93q11-42 43-43" fill="none" stroke="#16794b" stroke-width="12" stroke-linecap="round"/>
<ellipse cx="287" cy="83" rx="36" ry="18" fill="#16794b" transform="rotate(-22 287 83)"/>
</svg>"""


def fixture():
    pack = json.loads(SAMPLE.read_text(encoding="utf-8"))
    pack["provenance"].pop("approvedBy", None)
    pack["provenance"].pop("approvedAt", None)
    pack.pop("publishedAt", None)
    for origin in pack["provenance"]["assetOrigins"]:
        origin["reviewedByTeacher"] = False
    assets = [
        {
            "assetId": asset["id"], "role": variant["role"], "sha256": variant["sha256"],
            "previewUrl": f"/studio/api/schools/school_demo/stories/{pack['storyId']}/versions/1/assets/{asset['id']}/{variant['role']}",
        }
        for asset in pack["assets"] for variant in asset["variants"]
    ]
    return pack, assets


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    pack, assets = fixture()
    server = ThreadingHTTPServer(("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=str(STATIC)))
    worker = Thread(target=server.serve_forever, daemon=True)
    worker.start()
    state = {"status": "DRAFT", "assignments": [], "confirmed": 0}

    def intercept(route):
        path = urlsplit(route.request.url).path
        if "/assets/" in path:
            route.fulfill(status=200, content_type="image/svg+xml", body=APPLE_SVG)
            return
        if path.endswith("/csrf"):
            body = {"token": "fixture-csrf"}
        elif path.endswith("/me"):
            body = {"schools": [{"schoolId": "school_demo", "name": "Escola de demonstração", "role": "TEACHER"}]}
        elif path.endswith("/reviews"):
            body = [{"storyId": pack["storyId"], "version": 1, "title": pack["title"],
                     "state": state["status"], "revision": 1, "packSha256": "a" * 64}]
        elif path.endswith("/review"):
            body = {"storyId": pack["storyId"], "version": 1, "revision": 1,
                    "state": state["status"], "packSha256": "a" * 64,
                    "packJson": json.dumps(pack, ensure_ascii=False), "assets": assets}
        elif path.endswith("/classrooms"):
            body = [{"classroomId": "class_demo", "name": "Turma Sol"}]
        elif path.endswith("/device-pairing-codes") and route.request.method == "POST":
            route.fulfill(status=201, content_type="application/json", body=json.dumps({
                "pairingId": "pair_fixture_001", "code": "2345-6789",
                "expiresAt": "2026-09-17T22:00:00Z",
            }))
            return
        elif path.endswith("/preparation"):
            body = {"assignmentId": "assignment_fixture_001", "pairedCompatibleDevices": 1,
                    "recentlyConfirmedDevices": state["confirmed"], "freshnessHours": 24}
        elif path.endswith("/revoke") and route.request.method == "POST":
            state["assignments"] = []
            route.fulfill(status=204, body="")
            return
        elif path.endswith("/assignments"):
            if route.request.method == "POST":
                state["assignments"] = [{"assignmentId": "assignment_fixture_001",
                    "target": {"type": "CLASSROOM", "id": "class_demo"}}]
                body = state["assignments"][0]
            else:
                body = state["assignments"]
        elif path.endswith("/approve"):
            state["status"] = "APPROVED"
            body = {"state": "APPROVED"}
        elif path.endswith("/publish"):
            state["status"] = "PUBLISHED"
            body = {"state": "PUBLISHED"}
        else:
            route.fulfill(status=404, body="fixture route missing")
            return
        route.fulfill(status=200, content_type="application/json", body=json.dumps(body, ensure_ascii=False))

    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=True)
            for width, height, label in [(390, 844, "mobile"), (800, 1280, "tablet"), (1440, 1000, "desktop")]:
                state["status"] = "DRAFT"
                state["assignments"] = []
                state["confirmed"] = 0
                page = browser.new_page(viewport={"width": width, "height": height})
                errors = []
                page.on("pageerror", lambda error: errors.append(str(error)))
                page.on("dialog", lambda dialog: dialog.accept())
                page.route("**/studio/api/**", intercept)
                page.goto(f"http://127.0.0.1:{server.server_port}/index.html")
                expect(page.get_by_role("button", name="Revisar história")).to_be_visible()
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), f"list overflow: {label}"
                page.screenshot(path=OUTPUT / f"{label}-list.png", full_page=True)
                page.get_by_role("button", name="Gerar código do tablet").click()
                expect(page.get_by_text("2345-6789")).to_be_visible()
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), f"pairing overflow: {label}"
                page.screenshot(path=OUTPUT / f"{label}-pairing.png", full_page=True)
                page.get_by_role("button", name="Revisar história").click()
                expect(page.get_by_role("button", name="Aprovar esta versão")).to_be_disabled()
                expect(page.locator('[data-journey="review"] small')).to_have_text("Em revisão")
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), f"review overflow: {label}"
                media = page.locator("#story-assets img")
                expect(media).to_have_count(len(assets))
                for index in range(media.count()):
                    media.nth(index).scroll_into_view_if_needed()
                page.wait_for_function(
                    "Array.from(document.querySelectorAll('#story-assets img'))"
                    ".every(image => image.complete && image.naturalWidth > 0)"
                )
                page.locator("#review-title").scroll_into_view_if_needed()
                page.screenshot(path=OUTPUT / f"{label}-review.png", full_page=True)
                checks = page.locator("#story-assets input[type=checkbox]")
                assert checks.count() == len(assets)
                for index in range(checks.count()):
                    check = checks.nth(index)
                    check.scroll_into_view_if_needed()
                    expect(check).to_be_enabled()
                    check.check()
                expect(page.get_by_role("button", name="Aprovar esta versão")).to_be_enabled()
                page.get_by_role("button", name="Aprovar esta versão").click()
                expect(page.get_by_role("button", name="Publicar versão")).to_be_visible()
                page.get_by_role("button", name="Publicar versão").click()
                expect(page.locator('[data-journey="send"] small')).to_have_text("Escolher turma")
                expect(page.get_by_role("button", name="Enviar para esta turma")).to_be_visible()
                expect(page.get_by_role("button", name="Aprovar esta versão")).to_be_hidden()
                page.get_by_role("button", name="Enviar para esta turma").click()
                expect(page.locator('[data-journey="prepare"] small')).to_have_text("Confirmar aparelhos")
                expect(page.get_by_role("button", name="Enviar para esta turma")).to_be_disabled()
                expect(page.get_by_text(
                    "Turma Sol: disponível para baixar; 0 de 1 aparelhos confirmaram cache nas últimas 24 horas."
                )).to_be_visible()
                assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), f"assigned overflow: {label}"
                page.screenshot(path=OUTPUT / f"{label}-assigned.png", full_page=True)
                state["confirmed"] = 1
                page.get_by_role("button", name="Atualizar preparo dos aparelhos").click()
                expect(page.locator('[data-journey="prepare"] small')).to_have_text("1 aparelho(s) pronto(s)")
                expect(page.get_by_text(
                    "Turma Sol: disponível para baixar; 1 de 1 aparelhos confirmaram cache nas últimas 24 horas."
                )).to_be_visible()
                expect(page.get_by_text(
                    "Há confirmações recentes de cache; confira a quantidade por turma. Isso não indica uso pela criança."
                )).to_be_visible()
                expect(page.get_by_text("Preparo atualizado. Confira a quantidade de aparelhos por turma.")).to_be_visible()
                page.screenshot(path=OUTPUT / f"{label}-confirmed.png", full_page=True)
                page.get_by_role("button", name="Retirar da Turma Sol").click()
                expect(page.locator('[data-journey="send"] small')).to_have_text("Escolher turma")
                expect(page.get_by_text("Ainda não disponibilizada a nenhuma turma acessível.")).to_be_visible()
                expect(page.get_by_text(
                    "História retirada da Turma Sol. Aparelhos offline serão atualizados na próxima conexão."
                )).to_be_visible()
                page.screenshot(path=OUTPUT / f"{label}-withdrawn.png", full_page=True)
                assert not errors, (label, errors)
                page.close()
            browser.close()
    finally:
        server.shutdown()
        server.server_close()
    print("Estúdio: 3 larguras, pareamento, revisão, publicação e envio simulados com fixture sintética.")


if __name__ == "__main__":
    main()
