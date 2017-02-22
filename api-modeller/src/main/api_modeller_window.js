"use strict";
const Electron = require("electron");
const fs = require("fs");
const model_proxy_1 = require("./model_proxy");
require("api_modelling_framework");
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
    from_clj(x) {
        apiFramework.from_clj(x);
    }
    to_clj(x) {
        apiFramework.to_clj(x);
    }
    parseModelFile(type, fileLocation, cb) {
        let parser;
        if (type === "raml") {
            parser = new apiFramework.RAMLParser();
        }
        else if (type === "open-api") {
            parser = new apiFramework.OpenAPIParser();
        }
        var that = this;
        apiFramework.parse_file(parser, fileLocation, function (err, model) {
            if (err != null) {
                cb(err, null);
            }
            else {
                cb(null, new model_proxy_1.ModelProxy(model, type));
            }
        });
    }
}
ApiModellerWindow.functions = [
    "checkFile", "existsFile", "parseModelFile", "from_clj",
    "to_clj", "generateString"
];
exports.ApiModellerWindow = ApiModellerWindow;
//# sourceMappingURL=api_modeller_window.js.map