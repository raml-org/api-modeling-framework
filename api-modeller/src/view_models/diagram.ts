import {DocumentId, Fragment, Module, Document, Unit, DomainElement} from "../main/units_model";
import * as joint from "jointjs";
import Rect = joint.shapes.basic.Rect;
import Link = joint.dia.Link;
import Generic = joint.shapes.basic.Generic;
import Cell = joint.dia.Cell;
import Graph = joint.dia.Graph;
import Paper = joint.dia.Paper;
import * as domain from "../main/domain_model";
import {APIDocumentation} from "../main/domain_model";
import {EndPoint} from "../main/domain_model";
import {Operation} from "../main/domain_model";
import {Response} from "../main/domain_model";
import {Request} from "../main/domain_model";
import {Payload} from "../main/domain_model";
import * as utils from "../utils";

const CHAR_SIZE = 10;

const DEFAULT_DOMAIN_COLOR = "wheat";

const COLORS = {
    "encodes": "wheat",
    "declares": "lightpink",
    "references": "mediumseagreen",

    "unit": "azure",
    "domain": "beige",
    "declaration": "lavenderblush"
};

export class Diagram {
    public nodes: {[id:string]: Rect};
    public links: Link[];
    public paper: Paper;
    public scale = 1;
    public elements: (DocumentId & Unit)[];

    constructor(public level: "domain" | "document") {}

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
                    nodeSep: 100,
                    edgeSep: 100,
                    // clusterPadding: { top: 30, left: 10, right: 10, bottom: 10 },
                    rankDir: "LR"
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
        }, 100);
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
        this.makeNode(element, "unit");
        if (element.encodes != null) {
            const encodes = element.encodes;
            const encoded =  encodes.domain ? encodes.domain.root : undefined;
            if (encoded && this.level === "domain") {
                this.processDomainElement(element.id, encodes.domain ? encodes.domain.root : undefined);
            } else {
                this.makeNode(encodes, "domain");
                this.makeLink(element.id, encodes.id, "encodes");
            }
        }
    }

    private processModuleNode(element: Module) {
        this.makeNode(element, "unit");
        if (element.declares != null) {
            element.declares.forEach(declaration => {
                if (this.nodes[declaration.id] == null) {
                    this.makeNode(declaration, "declaration");
                }
                this.makeLink(element.id, declaration.id, "declares");
            });
        }
    }

    private processDocumentNode(document: Document) {
        this.makeNode(document, "unit");
        if (document.encodes != null) {
            const encodes = document.encodes;
            const encoded =  encodes.domain ? encodes.domain.root : undefined;
            if (encoded && this.level === "domain") {
                this.processDomainElement(document.id, encodes.domain ? encodes.domain.root : undefined);
            } else {
                this.makeNode(encodes, "domain");
                this.makeLink(document.id, encodes.id, "encodes");
            }
        }
        if (document.declares != null) {
            document.declares.forEach(declaration => {
                if (this.nodes[declaration.id] == null) {
                    this.makeNode(declaration, "declaration");
                }
                this.makeLink(document.id, declaration.id, "declares");
            })
        }
    }

    private processDomainElement(parentId: string,  element: domain.DomainElement | undefined) {
        if (element) {
            const domainKind = element.kind;
            switch(domainKind) {
                case "APIDocumentation": {
                    console.log("Processing APIDomain in graph " + element.id);
                    this.makeNode(element, "domain");
                    this.makeLink(parentId, element.id, "encodes");
                    ((element as APIDocumentation).endpoints||[]).forEach(endpoint => {
                        this.processDomainElement(element.id, endpoint);
                    });
                    break;
                }
                case "EndPoint": {
                    console.log("Processing EndPoint in graph " + element.id);
                    this.makeNode(element, "domain");
                    this.makeLink(parentId, element.id, "endpoint");
                    ((element as EndPoint).operations||[]).forEach(operation => {
                        this.processDomainElement(element.id, operation);
                    });
                    break;
                }
                case "Operation": {
                    console.log("Processing Operation in graph " + element.id);
                    this.makeNode({id: element.id, label: (element as Operation).method}, "domain");
                    this.makeLink(parentId, element.id, "supportedOperation");
                    ((element as Operation).requests||[]).forEach(request => {
                        this.processDomainElement(element.id, request);
                    });
                    ((element as Operation).responses||[]).forEach(response => {
                        this.processDomainElement(element.id, response);
                    });
                    break;
                }
                case "Response": {
                    console.log("Processing Response in graph " + element.id);
                    this.makeNode({id: element.id, label: (element as Response).status}, "domain");
                    this.makeLink(parentId, element.id, "returns");
                    ((element as Response).payloads||[]).forEach(payload => {
                        this.processDomainElement(element.id, payload);
                    });
                    break;
                }
                case "Request": {
                    console.log("Processing Request in graph " + element.id);
                    this.makeNode({id: element.id, label: "request"}, "domain");
                    this.makeLink(parentId, element.id, "expects");
                    ((element as Request).payloads||[]).forEach(payload => {
                        this.processDomainElement(element.id, payload);
                    });
                    break;
                }
                case "Payload": {
                    console.log("Processing Payload in graph " + element.id);
                    this.makeNode({id: element.id, label: (element as Payload).mediaType || "*/*"}, "domain");
                    this.makeLink(parentId, element.id, "payload");
                    this.processDomainElement(element.id, (element as Payload).schema);
                    break;
                }
                case "Schema": {
                    console.log("Processing Schema in graph " + element.id);
                    this.makeNode(element, "domain");
                    this.makeLink(parentId, element.id, "schema");
                    break;
                }
                default: {
                    this.makeNode(element, "domain");
                    break;
                }
            }
        } else {
            return undefined
        }
    }

    private processReferences(element: DocumentId & Unit) {
        if (element.references) {
            element.references.forEach(ref => {
                const reference = ref as DocumentId;
                if (reference.id && this.nodes[reference.id] != null) {
                    this.makeLink(element.id, reference.id, "references");
                }
            });
        }
    }

    private makeNode(node: {id: string, label: string}, kind: string) {
        const label = node.label != null ? node.label : utils.label(node.id);
        this.nodes[node.id] = new Rect({
            attrs: {
                rect: {
                    fill: COLORS[kind]
                },
                text: {
                    text: label,
                    fill: "black"
                }
            },
            position: {
                x: 0,
                y: 0
            },
            size: {
                width: label.length * CHAR_SIZE,
                height: 30
            }
        });
        console.log("GENERATING NODE " + node.id + " => " + this.nodes[node.id].id);
    }

    private makeLink(sourceId: string, targetId: string, label: string) {
        console.log("GENERATING LINK FROM " + this.nodes[sourceId].id + " TO " + this.nodes[targetId].id);
        this.links.push(new Link({
            source: {id: this.nodes[sourceId].id },
            target: {id: this.nodes[targetId].id },
            attrs: {
                ".marker-target": {
                    d: "M 10 0 L 0 5 L 10 10 z",
                    fill: COLORS[label] || DEFAULT_DOMAIN_COLOR,
                    stroke: COLORS[label] || DEFAULT_DOMAIN_COLOR
                },
                ".connection": { stroke: COLORS[label] || DEFAULT_DOMAIN_COLOR }
            },
            labels: [{
                position: 0.5,
                attrs: {
                    text: {
                        text: label
                    }
                }
            }]
        }));
    }
}