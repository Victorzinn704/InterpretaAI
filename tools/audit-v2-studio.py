#!/usr/bin/env python3
"""Percorre o protótipo docente e captura evidências visuais locais."""

from __future__ import annotations

import argparse
from pathlib import Path

from playwright.sync_api import Page, sync_playwright


VIEWPORTS = (
    (390, 844, "mobile"),
    (800, 1280, "tablet"),
    (1440, 1000, "desktop"),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8099/")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("output/screenshots/v2-studio"),
    )
    return parser.parse_args()


def assert_no_overflow(page: Page, context: str) -> None:
    fits = page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")
    assert fits, f"overflow horizontal: {context}"


def navigate(page: Page, viewport_width: int, target: str) -> None:
    nav = "#mobile-nav" if viewport_width <= 900 else "#desktop-nav"
    page.locator(f'{nav} button[data-page="{target}"]').click()


def capture_responsive_pages(browser, base_url: str, output: Path) -> None:
    for width, height, label in VIEWPORTS:
        page = browser.new_page(viewport={"width": width, "height": height})
        page.goto(base_url, wait_until="networkidle")
        assert_no_overflow(page, f"Hoje/{label}")
        if label in {"mobile", "tablet"}:
            page.screenshot(path=output / f"{label}-today.png", full_page=True)
        for target in ("stories", "classes", "reports", "today"):
            navigate(page, width, target)
            assert_no_overflow(page, f"{target}/{label}")
        if label == "mobile":
            navigate(page, width, "stories")
            page.screenshot(path=output / "mobile-stories.png", full_page=True)
            navigate(page, width, "reports")
            page.screenshot(path=output / "mobile-reports.png", full_page=True)
        page.close()


def exercise_teacher_flow(browser, base_url: str, output: Path) -> None:
    errors: list[str] = []
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    page.on(
        "console",
        lambda message: errors.append(f"console:{message.type}:{message.text}")
        if message.type == "error"
        else None,
    )
    page.on("pageerror", lambda error: errors.append(f"page:{error}"))
    page.goto(base_url, wait_until="networkidle")
    assert page.locator("#page-title").inner_text() == "Hoje"
    page.screenshot(path=output / "desktop-today.png", full_page=True)

    navigate(page, 1440, "stories")
    page.get_by_role("button", name="+ Criar história").click()
    page.get_by_role("button", name="Continuar").click()
    page.get_by_role("button", name="Usar maçã de demonstração").click()
    page.get_by_role("button", name="Continuar").click()
    page.locator("#target-word").fill("MACA")
    page.get_by_role("button", name="Criar proposta").click()
    page.get_by_role("button", name="Revisar história").click()
    page.locator("#review-word").fill("MAÇÃ")
    page.get_by_role("button", name="Salvar palavra").click()
    assert page.get_by_text(
        "Palavra atualizada em todas as cenas relacionadas."
    ).is_visible()
    page.get_by_role("button", name="Refazer só esta imagem").click()
    assert page.get_by_text(
        "Nova variante solicitada apenas para esta imagem."
    ).is_visible()
    page.wait_for_timeout(2300)
    page.get_by_role("button", name="Tablet").click()
    page.screenshot(path=output / "desktop-review-tablet.png", full_page=True)
    page.get_by_role("button", name="Continuar para publicar").click()
    page.get_by_role("button", name="Publicar e preparar aparelhos").click()
    assert page.get_by_text(
        "Versão publicada. Preparação automática iniciada."
    ).is_visible()

    navigate(page, 1440, "classes")
    assert page.get_by_text("Confirmado pelo aparelho").is_visible()
    navigate(page, 1440, "reports")
    page.locator("#observation").fill(
        "A turma justificou a escolha usando a pista da árvore."
    )
    page.get_by_role("button", name="Salvar observação").click()
    assert page.get_by_text("Observação salva com histórico.").is_visible()
    page.wait_for_timeout(2300)
    page.screenshot(path=output / "desktop-reports.png", full_page=True)
    assert not errors, errors
    page.close()

    tablet = browser.new_page(viewport={"width": 800, "height": 1280})
    tablet.goto(base_url, wait_until="networkidle")
    navigate(tablet, 800, "stories")
    tablet.get_by_role("button", name="+ Criar história").click()
    tablet.get_by_role("button", name="Continuar").click()
    tablet.get_by_role("button", name="Usar maçã de demonstração").click()
    tablet.get_by_role("button", name="Continuar").click()
    tablet.get_by_role("button", name="Criar proposta").click()
    tablet.get_by_role("button", name="Revisar história").click()
    tablet.get_by_role("button", name="Tablet").click()
    assert_no_overflow(tablet, "revisão/tablet")
    tablet.screenshot(path=output / "tablet-review.png", full_page=True)
    tablet.close()


def main() -> None:
    args = parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        capture_responsive_pages(browser, args.base_url, args.output)
        exercise_teacher_flow(browser, args.base_url, args.output)
        browser.close()
    print("Estúdio v2: fluxo docente e responsividade aprovados tecnicamente.")


if __name__ == "__main__":
    main()
