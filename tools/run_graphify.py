import json, os
from pathlib import Path

REPO = "D:/Minecraft/CaidaDeLosDioses"
NAMES = {
    0: "Eventos de tick y coinflip", 1: "Red: paquetes de habilidades", 2: "Daño elemental y balance de armas",
    3: "Razas y seleccion de clase", 4: "Comandos (/heaven, /hell, razas)", 5: "Pilas elementales y procs",
    6: "Eventos de jugador y stats", 7: "Capabilities y reliquias", 8: "Registro de habilidades y HUD",
    9: "Arbol de habilidades e intro", 10: "Items del apostata", 11: "Render de armaduras y capas (wintry glow)",
    12: "Clases del jugador", 13: "Anima Spirit (entidad)", 14: "Habilidades raciales", 15: "Pantalla del arbol de habilidades",
    16: "Mixins", 17: "Checkpoints", 18: "Modelos de dioses y alas", 19: "Reglas de daño (proteccion magica, frio)",
    20: "Categorias JEI", 21: "Jefe God (GodBossEntity)",
}


def main():
    os.chdir(REPO)
    Path("graphify-out").mkdir(exist_ok=True)
    from graphify.detect import detect
    from graphify.extract import collect_files, extract
    from graphify.build import build_from_json
    from graphify.cluster import cluster, score_all
    from graphify.analyze import god_nodes, surprising_connections, suggest_questions
    from graphify.report import generate
    from graphify.export import to_json

    SRC = Path("src")
    det = detect(SRC)
    code = []
    for f in det["files"].get("code", []):
        code.extend(collect_files(Path(f)) if Path(f).is_dir() else [Path(f)])
    ext = extract(code, cache_root=SRC, root=SRC)
    ext.setdefault("hyperedges", [])
    G = build_from_json(ext, root=str(SRC), directed=False)
    comms = cluster(G)
    coh = score_all(G, comms)
    gods = god_nodes(G)
    sur = surprising_connections(G, comms)
    labels = {c: NAMES.get(c, f"Comunidad {c}") for c in comms}
    q = suggest_questions(G, comms, labels)
    if not to_json(G, comms, "graphify-out/graph.json", community_labels=labels, force=True) if False else not to_json(G, comms, "graphify-out/graph.json", community_labels=labels):
        raise SystemExit("graph.json no escrito")
    Path("graphify-out/GRAPH_REPORT.md").write_text(
        generate(G, comms, coh, labels, gods, sur, det, {"input": 0, "output": 0}, str(SRC), suggested_questions=q), encoding="utf-8")
    print("grafo:", G.number_of_nodes(), "nodos", G.number_of_edges(), "aristas", len(comms), "comunidades")


if __name__ == "__main__":
    main()
