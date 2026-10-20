# Path of Ascension (mod tcorigenes / testamentodelacarne / elementaldamage, Forge 1.20.1)

## Grafo del codigo (graphify) - LEERLO ANTES DE BUSCAR A MANO
- Al empezar una sesion en este proyecto, leer `graphify-out/GRAPH_REPORT.md` (comunidades con nombre, nodos centrales, conexiones sorprendentes).
- Para cualquier pregunta sobre donde esta algo o como se conecta: `graphify query "<pregunta>"` (o Grep sobre `graphify-out/graph.json`) ANTES de abrir archivos uno por uno.
- Despues de cambios grandes de codigo, regenerarlo: `python tools/run_graphify.py` (detect + extract AST de `src/` + build + cluster + report; es solo codigo, no usa LLM ni API key). Los nombres de las comunidades se ponen a mano en el dict NAMES del script.
- `graphify-out/` no se sube al repo (esta en .gitignore).
