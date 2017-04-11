import {ModelLevel, ModelProxy} from "./model_proxy";
import {LexicalInfo} from "./model_utils";

const apiFramework = window["api_modelling_framework"].core;

const ramlGenerator = new apiFramework.__GT_RAMLGenerator();
const openAPIGenerator = new apiFramework.__GT_OpenAPIGenerator();
const apiModelGenerator = new apiFramework.__GT_APIModelGenerator();

export type LexicalSyntaxes = "raml" | "open-api";

export class LexicalInfoGenerator {
    private lexicalInfoMaps: {[syntax: string]: any} = {};
    public text: {[syntax: string]: string} = {};

    constructor(public model: ModelProxy) {
        this.text[this.key(model.sourceType, "document")] = model.text();
    }

    private key(syntax: LexicalSyntaxes, level: ModelLevel) {
        return `${syntax}::${level}`;
    }

    getText(syntax: LexicalSyntaxes, level: ModelLevel) {
        return this.text[this.key(syntax, level)];
    }

    lexicalInfoFor(id: string, syntax: LexicalSyntaxes, level: ModelLevel, cb:(err, res) => void) {
        if (this.model.sourceType === syntax && level === "document") {
            // in this case the lexical info is native to the generated model, we can retrieve it directly
            cb(null, this.model.elementLexicalInfo(id));
        } else {
            debugger;
            // We need to generate the output text and generate the lexical info
            this.exportLexicalInfo(id, syntax, level, (err, lexicalInfo) => {
                if (err == null){
                    cb(null, lexicalInfo ? lexicalInfo.lexical : null);
                } else {
                    cb(err, null);
                }
            });
        }
    }

    exportLexicalInfo(id: string, syntax: LexicalSyntaxes, level: ModelLevel, cb:(err, res) => void) {
        if (this.lexicalInfoMaps[this.key(syntax,level)] != null) {
            cb(null, this.lexicalInfoMaps[this.key(syntax,level)][id]);
        } else {
            this.generateLexicalInfo(syntax, level, (err, _) => {
                if (err) {
                    cb(err, null);
                } else {
                    this.exportLexicalInfo(id, syntax, level, cb)
                }
            })
        }
    }

    generateLexicalInfo(syntax: LexicalSyntaxes, level: ModelLevel, cb:(err, res) => void) {
        if (this.lexicalInfoMaps[this.key(syntax, level)] != null) {
            cb(null, this.lexicalInfoMaps[this.key(syntax, level)]);
        } else {
            this.generateSyntax(syntax, level, (err, textWithIds) => {
                if (err) {
                    cb(err, null);
                } else {
                    const header = this.findHeader(syntax, textWithIds);
                    const mappingInfo = this.generateIdsMap(textWithIds, syntax);
                    const mapping = mappingInfo.mapping;
                    const ast = this.generateCleanAST(mappingInfo.ast, level, header, syntax);
                    this.lexicalInfoMaps[this.key(syntax, level)] = this.fetchLexicalInfo(ast, mapping, header == null ? 0 : 1);
                    cb(null, this.lexicalInfoMaps[this.key(syntax, level)]);
                }
            });
        }
    }

    findHeader(syntax, text) {
        if (syntax === "raml") {
            const maybeHeader = text.split("\n")[0];
            if(maybeHeader.indexOf("#") === 0) {
                return maybeHeader;
            } else {
                return undefined;
            }
        } else {
            return undefined;
        }
    }
    generateSyntax(syntax: LexicalSyntaxes, level: ModelLevel, cb:(err, res) => void) {
        var generator = null;
        if (syntax === "raml") {
            generator = ramlGenerator;
        } else {
            generator = openAPIGenerator;
        }
        let liftedModel = (level === "document") ? this.model.documentModel() : this.model.domainModel();
        apiFramework.generate_string(
            generator,
            this.model.location(),
            liftedModel,
            {"generate-amf-info": true},
            cb
        );
    }

    generateIdsMap(textWithIds: string, syntax: LexicalSyntaxes) {
        let idKey = null;
        let classKey = null;
        let parser = null;
        if (syntax === "raml") {
            idKey = "(amf-id)";
            classKey = "(amf-class)";
            parser = window['JS_YAML'].loadYaml;
        } else {
            idKey = "x-amf-id";
            classKey = "x-amf-class"
            parser = function(text) { return window['JS_AST']("", text); };
        }

        if (parser != null && idKey != null && classKey != null) {
            // This will not follow $refs/!includes
            let parsed = parser(textWithIds);
            let acc = {};
            let cleanAST = this.traceIds(idKey, classKey, parsed, [], acc);
            cleanAST = this.removeLocations(cleanAST);
            return {
                ast: cleanAST,
                mapping: acc
            };
        } else {
            throw new Error("Cannot find syntax parser info for syntax " + syntax);
        }
    }

    traceIds(idKey, classKey, node, path, acc) {
        if (node == null){
            return node;
        } else if (Object.prototype.toString.call(node) === '[object Array]') {
            for (let i = 0; i < node.length ; i++) {
                this.traceIds(idKey, classKey, node[i], path.concat([i]), acc);
            }
        } else if (typeof(node) === "object") {
            if (node[idKey] != null) {
                acc[node[idKey]] = {
                    path: path.concat([]),
                    classId: node[classKey]
                };
                delete node[idKey];
                delete node[classKey];
                delete node["__location__"];
            }
            for (let p in node) {
                if (node.hasOwnProperty(p)) {
                    this.traceIds(idKey, classKey, node[p], path.concat([p]), acc);
                }
            }
        }
        return node;
    }

    removeLocations(node) {
        if (node == null){
            return node;
        } else if (Object.prototype.toString.call(node) === '[object Array]') {
            for (let i = 0; i < node.length ; i++) {
                this.removeLocations(node[i]);
            }
        } else if (typeof(node) === "object") {
            delete node["__location__"];
            for (let p in node) {
                if (node.hasOwnProperty(p)) {
                    this.removeLocations(node[p]);
                }
            }
        }
        return node;
    }

    generateCleanAST(ast, level: ModelLevel, header: string | undefined, syntax: LexicalSyntaxes) {
        let generator = null;
        let parser = null;
        if (syntax === "raml") {
            generator = window['JS_YAML'].dump;
            parser = window['JS_YAML'].loadYaml;
        } else {
            generator = function(ast) { return JSON.stringify(ast, null, 2); };
            parser = function(text) { return window['JS_AST']("", text); };
        }

        let textWithoutIds = generator(ast);
        const finalText = header != null ? (header + "\n" + textWithoutIds) : textWithoutIds;
        this.text[this.key(syntax,level)] = finalText;
        return parser(textWithoutIds);
    }

    fetchLexicalInfo(ast, mapping, offset = 0) {
        for (let id in mapping) {
            const nodeMapping = mapping[id];

            const res = this.findLexicalInfoInPath(ast, nodeMapping.path);
            if (res != null) {
                const lexical = new LexicalInfo(
                    parseInt(res["start-line"]) + offset,
                    parseInt(res["start-column"]),
                    parseInt(res["start-index"]),
                    parseInt(res["end-line"]) + offset,
                    parseInt(res["end-column"]),
                    parseInt(res["end-index"])
                );
                nodeMapping["lexical"] = lexical
            } else {
                nodeMapping["lexical"] = null;
            }
        }

        return mapping;
    }

    findLexicalInfoInPath(ast, path) {
        if (path.length === 0) {
            return ast["__location__"];
        } else {
            const first = path[0];
            const rest = path.slice(1, path.length);
            return this.findLexicalInfoInPath(ast[first], rest);
        }
    }
}