# Créditos, dependências e componentes de terceiros

Este inventário atende ao dever de crédito previsto no item 9.2 do regulamento do HACKTUDO 2026.
Ele registra dependências diretas; dependências transitivas continuam submetidas às licenças
publicadas em seus pacotes. Nenhum item abaixo transfere ao projeto marca, autoria ou endosso do
respectivo titular.

## Android

| Componente | Versão usada | Licença/termos | Fonte e download |
|---|---:|---|---|
| Android Gradle Plugin | 8.7.3 | Apache 2.0 / termos do Android SDK | [Google Maven](https://maven.google.com/web/index.html#com.android.tools.build:gradle:8.7.3) |
| Kotlin e plugin Compose | 2.0.21 | Apache 2.0 | [Kotlin](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21) |
| AndroidX Core | 1.15.0 | Apache 2.0 | [AndroidX Core](https://developer.android.com/jetpack/androidx/releases/core#1.15.0) |
| Activity Compose | 1.10.0 | Apache 2.0 | [AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity#1.10.0) |
| Lifecycle | 2.8.7 | Apache 2.0 | [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.8.7) |
| Jetpack Compose BOM | 2024.12.01 | Apache 2.0 | [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping) |
| Material 3 / Compose UI | versão resolvida pelo BOM | Apache 2.0 | [Compose](https://developer.android.com/jetpack/androidx/releases/compose) |
| CameraX | 1.4.1 | Apache 2.0 | [CameraX](https://developer.android.com/jetpack/androidx/releases/camera#1.4.1) |
| ML Kit Text Recognition | 16.0.1 | Termos do Google ML Kit/Google APIs | [ML Kit](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) |
| ML Kit Image Labeling | 17.0.9 | Termos do Google ML Kit/Google APIs | [ML Kit](https://developers.google.com/ml-kit/vision/image-labeling/android) |
| JUnit | 4.13.2 | Eclipse Public License 1.0 | [JUnit 4](https://github.com/junit-team/junit4/releases/tag/r4.13.2) |
| AndroidX Test/Espresso | 1.2.1 / 3.6.1 | Apache 2.0 | [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test) |

## Servidor Java e provedores

| Componente | Versão usada | Licença/termos | Fonte e download |
|---|---:|---|---|
| Spring Boot | 3.5.16 | Apache 2.0 | [Spring Boot](https://github.com/spring-projects/spring-boot/releases/tag/v3.5.16) |
| LangChain4j | 1.20.0 | Apache 2.0 | [LangChain4j](https://github.com/langchain4j/langchain4j/releases/tag/1.20.0) |
| LangChain4j Google GenAI | 1.20.0-beta30 | Apache 2.0; o serviço Google tem termos próprios | [Maven Central](https://central.sonatype.com/artifact/dev.langchain4j/langchain4j-google-genai/1.20.0-beta30) |
| LangChain4j OpenAI-compatible | 1.20.0 | Apache 2.0; usado pelo adaptador NVIDIA NIM | [Maven Central](https://central.sonatype.com/artifact/dev.langchain4j/langchain4j-open-ai/1.20.0) |
| Google Cloud Text-to-Speech client | BOM 26.88.1 | Apache 2.0; o serviço Google tem termos próprios | [Google Cloud Java](https://github.com/googleapis/google-cloud-java) |
| Ollama | instalação externa | MIT | [Ollama](https://github.com/ollama/ollama) |
| Qwen 2.5 1.5B | imagem `qwen2.5:1.5b` | Apache 2.0 | [Qwen2.5](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct) / [Ollama](https://ollama.com/library/qwen2.5:1.5b) |
| Kokoro | 0.9.4 | Apache 2.0 | [Kokoro](https://github.com/hexgrad/kokoro) / [PyPI](https://pypi.org/project/kokoro/0.9.4/) |
| NumPy | 2.2.6 | BSD 3-Clause | [NumPy](https://github.com/numpy/numpy/releases/tag/v2.2.6) |
| python-soundfile | 0.13.1 | BSD 3-Clause | [SoundFile](https://github.com/bastibe/python-soundfile/releases/tag/0.13.1) |
| Gemini API | provedor alternativo, sem chave no Git | Termos do Google APIs/Gemini | [Google AI for Developers](https://ai.google.dev/gemini-api/docs) |
| NVIDIA NIM API Catalog | adaptador opcional, sem chave no Git | NVIDIA API Trial Terms e termos de cada modelo | [NVIDIA NIM](https://build.nvidia.com/) |
| Gemma 4 31B IT | modelo remoto opcional | NVIDIA Open Model Agreement; indicação Apache 2.0 no catálogo | [Model card](https://build.nvidia.com/google/gemma-4-31b-it/modelcard) |
| Kimi K3 | modelo remoto opcional | NVIDIA Open Model Agreement; Modified MIT | [Model card](https://build.nvidia.com/moonshotai/kimi-k3/modelcard) |
| Mistral Nemotron | modelo remoto opcional | NVIDIA API Trial Terms; licenciamento de implantação deve ser confirmado com a Mistral | [Catálogo](https://build.nvidia.com/mistralai/mistral-nemotron) |
| Nemotron 3 Ultra 550B A55B | modelo remoto opcional | OpenMDW 1.1 e NVIDIA API Trial Terms | [Model card](https://build.nvidia.com/nvidia/nemotron-3-ultra-550b-a55b/modelcard) |

O padrão anterior `qwen2.5:3b` foi removido em 13/09/2026 porque 3B usa licença própria e mais
restritiva. O padrão 1.5B preserva o MVP e está entre as variantes que a equipe Qwen declara sob
Apache 2.0. Antes de produção, devem ser arquivadas cópias das licenças de todos os pacotes e pesos
efetivamente implantados.

Os quatro modelos NVIDIA acima estão catalogados no adaptador, mas não são incorporados ao APK nem
declarados como validados com dados infantis. O modelo efetivamente usado em cada entrega deve ser
registrado junto com seus termos vigentes e evidência de smoke test.

## Ferramentas que não são incorporadas ao APK

Gradle, Android SDK, JDK, Python, Cloudflared e ferramentas de geração/edição são instrumentos de
desenvolvimento. Sua utilização não significa que seus binários estejam redistribuídos dentro do
APK. Ollama, Kokoro e seus pesos rodam fora do Android no servidor da demonstração.

## Conteúdo visual, sonoro e assistência por IA

Os nove JPEGs e quatro efeitos WAV distribuídos no APK estão enumerados, com hash e situação de
proveniência, em [docs/ASSET_PROVENANCE.md](docs/ASSET_PROVENANCE.md). A equipe deve assinar a
declaração desse arquivo antes do pitching. Enquanto faltar essa confirmação, esses ativos não devem
ser apresentados como juridicamente liberados por auditoria independente.

IA generativa e assistência de programação podem ter apoiado ilustrações, texto, código e
documentação. A equipe humana definiu o problema, selecionou e revisou as saídas e responde pelo
resultado entregue. O uso de ferramenta não autoriza imitação de personagens, marcas, vozes ou
obras de terceiros. Veja [docs/AUTORIA_ORIGINALIDADE_E_IA.md](docs/AUTORIA_ORIGINALIDADE_E_IA.md).
