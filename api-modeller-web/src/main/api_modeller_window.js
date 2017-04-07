"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Electron = require("electron");
const fs = require("fs");
const model_proxy_1 = require("./model_proxy");
const apiFramework = global["api_modelling_framework"].core;
class ApiModellerWindow extends Electron.BrowserWindow {
    static wrap(o) {
        ApiModellerWindow.functions.forEach(f => {
            o[f] = ApiModellerWindow.prototype[f];
        });
    }
    checkFile(cb) {
        Electron.dialog.showOpenDialog((fileNames) => {
            const fileName = (fileNames || [])[0];
            if (fileName != null) {
                console.log(`Loading fileName ${fileName}`);
                this.existsFile(fileName, cb);
            }
            else {
                cb(null, null);
            }
        });
    }
    ;
    existsFile(fileName, cb) {
        if (fs.existsSync(fileName)) {
            cb(null, fileName);
        }
        else {
            cb(new Error("File does not exists"), null);
        }
    }
    parseModelFile(type, fileLocation, cb) {
        console.log("PARSING FILE " + fileLocation + " TYPE " + type);
        let parser;
        if (type === "raml") {
            parser = new apiFramework.RAMLParser();
        }
        else if (type === "open-api") {
            parser = new apiFramework.OpenAPIParser();
        }
        apiFramework.parse_file(parser, fileLocation, function (err, model) {
            if (err != null) {
                console.log("Error parsing file");
                console.log(err);
                cb(err, null);
            }
            else {
                cb(null, new model_proxy_1.ModelProxy(model, type));
            }
        });
    }
    from_clj(x) {
        return apiFramework.fromClj(x);
    }
    to_clj(x) {
        console.log("********** TO CLJ");
        try {
            return apiFramework.toClj(x);
        }
        catch (e) {
            console.log("ERROR");
            console.log(e);
            return x;
        }
    }
}
ApiModellerWindow.functions = [
    "checkFile", "existsFile", "parseModelFile", "generateString"
];
exports.ApiModellerWindow = ApiModellerWindow;
//# sourceMappingURL=api_modeller_window.js.map