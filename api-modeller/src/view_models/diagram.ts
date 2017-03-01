import {DocumentId, Fragment, Module, Document, Unit} from "../main/units_model";
import * as joint from "jointjs";
import Rect = joint.shapes.basic.Rect;
import Link = joint.dia.Link;
import Generic = joint.shapes.basic.Generic;
import Cell = joint.dia.Cell;
import Graph = joint.dia.Graph;
import Paper = joint.dia.Paper;

const CHAR_SIZE = 10;

export class Diagram {
    public nodes: {[id:string]: Rect};
    public links: Link[];
    public paper: Paper;
    public scale = 1;
    public elements: (DocumentId & Unit)[];

    constructor() {}

    process(elements: (DocumentId & Unit)[]) {
        this.nodes = {};
        this.links = [];
        this.elements = elements;
        this.elements.forEach(element => {
            if (element.kind === "Fragment") {
                this.processFragmentNode(element as Fragment);
            } else if (element.kind === "Module") {
                this.processModuleNode(element as Module);
            } else if (element.kind === "Document") {
                this.processDocumentNode(element as Document)
            }
        });

        this.elements.forEach(element => {
            this.processReferences(element);
        })
    }

    render(div: string) {
        setTimeout(() => {
        const diagContainer = document.getElementById(div);
        if (diagContainer != null) {

            let classes: Cell[] = [];
            for (let p in this.nodes) {
                classes.push(this.nodes[p]);
            }

            let cells: Cell[] = (classes).concat(this.links);
            if (joint.layout != null) {
                joint.layout.DirectedGraph.layout(cells, {
                    marginX: 50,
                    marginY: 50,
                    nodeSep: 50,
                    edgeSep: 50,
                    rankDir: "TB"
                });
            }
            const maxX = cells
                .map(c => c['attributes'].position ? (c['attributes'].position.x + c['attributes'].size.width) : 0)
                .sort((a, b) => {
                    if (a > b) {
                        return -1;
                    } else if (a < b) {
                        return 1;
                    } else {
                        return 0;
                    }
                })[0];
            const maxY = cells
                .map(c => c['attributes'].position ? (c['attributes'].position.y + c['attributes'].size.height) : 0)
                .sort((a, b) => {
                    if (a > b) {
                        return -1;
                    } else if (a < b) {
                        return 1;
                    } else {
                        return 0;
                    }
                })[0];
            cells.map(c => c['attributes'].position ? c['attributes'].position.x : 0);


            const graph: any = new Graph();
            let width = maxX + 100;
            let height = maxY + 100;

            if (diagContainer != null) {
                diagContainer.innerHTML = "";

                let minWidth = diagContainer.clientWidth;
                let minHeight = diagContainer.clientHeight;
                const options = {
                    el: diagContainer,
                    width: (minWidth > width ? minWidth : width),
                    height: (minHeight > height ? minHeight : height),
                    gridSize: 1,
                    highlighting: false
                };
                options["model"] = graph;
                this.paper = new Paper(options);

                /*
                 paper.on("cell:pointerdown",
                 (cellView, evt, x, y) => {
                 let query = cellView.model.attributes.attrs.query as QueryNode;
                 if (query) {
                 openQueryTransformerWindow(query.toJson());
                 }
                 }
                 );
                 */
                graph.addCells(cells);
                //this.paper.fitToContent();
                //this.resetZoom();
                return true;
            }
        }
        }, 1000);
    }

    paperScale(paper, sx, sy) {
        paper.scale(sx, sy);
    }

    zoomOut() {
        this.scale -= 0.05;
        this.paperScale(this.paper, this.scale, this.scale);
    }

    zoomIn() {
        this.scale += 0.05;
        this.paperScale(this.paper, this.scale, this.scale);
    }

    resetZoom() {
        this.scale = 1;
        this.paperScale(this.paper, this.scale, this.scale);
    }

    private processFragmentNode(element: Fragment) {
        this.nodes[element.id] = new Rect({
            attrs: {
                rect: {
                    fill: '#31D0C6'
                },
                text: {
                    text: element.label
                }
            },
            position: {
                x: 0,
                y: 0
            },
            size: {
                width: element.label.length * CHAR_SIZE,
                height: 30
            }
        });
        if (element.encodes != null) {
            const encodes = element.encodes;
            this.nodes[encodes.id] = new Rect({
                position: {
                    x: 0,
                    y: 0
                },
                attrs: {
                    rect: {
                        fill: '#31D0C6'
                    },
                    text: {
                        text: encodes.label
                    }
                },
                size: { width: encodes.label.length * CHAR_SIZE, height: 30 }
            });
            this.links.push(new Link({
                source: {id: this.nodes[element.id].id},
                target: {id: this.nodes[element.encodes.id].id},
                labels: [{
                    position: 0.5,
                    attrs: {
                        text: {text: "encodes"}
                    }
                }]
            }));
        }
    }

    private processModuleNode(element: Module) {
        this.nodes[element.id] = new Rect({
            attrs: {
                rect: {
                    fill: '#31D0C6'
                },
                text: {
                    text: element.label
                }
            },
            position: {
                x: 0,
                y: 0
            },
            size: {
                width: element.label.length * CHAR_SIZE,
                height: 30
            }
        });
        if (element.declares != null) {
            element.declares.forEach(declaration => {
                if (this.nodes[declaration.id] == null) {
                    this.nodes[declaration.id] = new Rect({
                        attrs: {
                            rect: {
                                fill: '#31D0C6'
                            },
                            text: {
                                text: declaration.label
                            }
                        },
                        position: {
                            x: 0,
                            y: 0
                        },
                        size: {
                            width: declaration.label.length * CHAR_SIZE,
                            height: 30
                        }
                    });
                }
                this.links.push(new Link({
                    source: {id: this.nodes[element.id].id},
                    target: {id: this.nodes[declaration.id].id},
                    labels: [{
                        position: 0.5,
                        attrs: {
                            text: {text: "declares"}
                        }
                    }]
                }));
            })
        }
    }

    private processDocumentNode(document: Document) {
        this.nodes[document.id] = new Rect({
            attrs: {
                rect: {
                    fill: '#31D0C6'
                },
                text: {
                    text: document.label
                }
            },
            position: {
                x: 0,
                y: 0
            },
            size: {
                width: document.label.length * CHAR_SIZE,
                height: 30
            }
        });
        if (document.encodes != null) {
            const encodes = document.encodes;
            this.nodes[encodes.id] = new Rect({
                attrs: {
                    rect: {
                        fill: '#31D0C6'
                    },
                    text: {
                        text: encodes.label
                    }
                },
                position: {
                    x: 0,
                    y: 0
                },
                size: {
                    width: encodes.label.length * CHAR_SIZE,
                    height: 30
                }
            });
            this.links.push(new Link({
                source: {id: this.nodes[document.id].id},
                target: {id: this.nodes[document.encodes.id].id},
                labels: [{
                    position: 0.5,
                    attrs: {
                        text: { text: "encodes" }
                    }
                }]
            }));
        }
        if (document.declares != null) {
            document.declares.forEach(declaration => {
                if (this.nodes[declaration.id] == null) {
                    this.nodes[declaration.id] = new Rect({
                        attrs: {
                            rect: {
                                fill: '#31D0C6'
                            }, text: {
                                text: declaration.label
                            }
                        },
                        position: {
                            x: 0,
                            y: 0
                        },
                        size: {
                            width: declaration.label.length * CHAR_SIZE,
                            height: 30
                        }
                    });
                }
                this.links.push(new Link({
                    source: {id: this.nodes[document.id].id},
                    target: {id: this.nodes[declaration.id].id},
                    labels: [{
                        position: 0.5,
                        attrs: {
                            text: {text: "declares"}
                        }
                    }]
                }));
            })
        }
    }

    private processReferences(element: DocumentId & Unit) {
        if (element.references) {
            element.references.forEach(ref => {
                const reference = ref as DocumentId;
                if (reference.id && this.nodes[reference.id] != null) {
                    this.links.push(new Link({
                        source: {id: this.nodes[element.id].id },
                        target: {id: this.nodes[reference.id].id },
                        labels: [{
                            position: 0.5,
                            attrs: {
                                text: {text: "references"}
                            }
                        }]
                    }));
                }
            });
        }
    }
}