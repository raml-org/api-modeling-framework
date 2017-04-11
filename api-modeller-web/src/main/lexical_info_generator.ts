import {ModelLevel, ModelProxy} from "./model_proxy";
import {LexicalInfo} from "./model_utils";

const apiFramework = window["api_modelling_framework"].core;

const ramlGenerator = new apiFramework.__GT_RAMLGenerator();
const openAPIGenerator = new apiFramework.__GT_OpenAPIGenerator();
const apiModelGenerator = new apiFramework.__GT_APIModelGenerator();

export class LexicalInfoGenerator {
    private lexicalInfoMaps: {[syntax: string]: any} = {};
    public text: {[syntax: string]: string} = {};

    constructor(public model: ModelProxy) {
        this.text[model.sourceType] = model.text();
    }

    lexicalInfoFor(id: string, syntax: string, level: ModelLevel, cb:(err, res) => void) {
        if (this.model.sourceType === syntax) {
            // in this case the lexical info is native to the generated model, we can retrieve it directly
            cb(null, this.model.elementLexicalInfo(id));
        } else {
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

    exportLexicalInfo(id: string, syntax: string, level: ModelLevel, cb:(err, res) => void) {
        if (this.lexicalInfoMaps[syntax] != null) {
            cb(null, this.lexicalInfoMaps[syntax][id]);
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

    generateLexicalInfo(syntax: string, level: ModelLevel, cb:(err, res) => void) {
        if (this.lexicalInfoMaps[syntax] != null) {
            cb(null, this.lexicalInfoMaps[syntax]);
        } else {
            this.generateSyntax(syntax, level, (err, textWithIds) => {
                if (err) {
                    cb(err, null);
                } else {
                    const mappingInfo = this.generateIdsMap(textWithIds, syntax);
                    const mapping = mappingInfo.mapping;
                    var ast = this.generateCleanAST(mappingInfo.ast, syntax);
                    this.lexicalInfoMaps[syntax] = this.fetchLexicalInfo(ast, mapping);
                    cb(null, this.lexicalInfoMaps[syntax]);
                }
            });
        }
    }

    generateSyntax(syntax, level: ModelLevel, cb:(err, res) => void) {
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

    generateIdsMap(textWithIds: string, syntax) {
        var idKey = null;
        var classKey = null;
        var parser = null;
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
            var parsed = parser(textWithIds);
            var acc = {};
            var cleanAST = this.traceIds(idKey, classKey, parsed, [], acc);
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

    generateCleanAST(ast, syntax) {
        var generator = null;
        var parser = null;
        if (syntax === "raml") {
            generator = window['JS_YAML'].dump;
            parser = window['JS_YAML'].loadYaml;
        } else {
            generator = function(ast) { return JSON.stringify(ast, null, 2); };
            parser = function(text) { return window['JS_AST']("", text); };
        }

        var textWithoutIds = generator(ast);
        this.text[syntax] = textWithoutIds;
        return parser(textWithoutIds);
    }

    fetchLexicalInfo(ast, mapping) {
        for (let id in mapping) {
            const nodeMapping = mapping[id];

            const res = this.findLexicalInfoInPath(ast, nodeMapping.path);
            if (res != null) {
                const lexical = new LexicalInfo(
                    parseInt(res["start-line"]),
                    parseInt(res["start-column"]),
                    parseInt(res["start-index"]),
                    parseInt(res["end-line"]),
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