"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Electron = require("electron");
const api_modeller_window_1 = require("./main/api_modeller_window");
const client = require('electron-connect').client;
class ApiModellerApp {
    constructor() {
        // require("./utils/persistence").clear();
        Electron.app.on("ready", () => {
            this.setupApplicationMenu();
            // capture currently focused window before creating new one (which will get focus)
            const newWindow = new Electron.BrowserWindow();
            api_modeller_window_1.ApiModellerWindow.wrap(newWindow);
            newWindow.setPosition(100, 100);
            newWindow.setSize(1800, 1000);
            newWindow.webContents.openDevTools();
            newWindow.loadURL(`file://${__dirname}/../public/index.html`);
            client.create(newWindow);
        });
    }
    setupApplicationMenu() {
        const template = [
            {
                label: "Edit",
                submenu: [
                    { label: "Undo", accelerator: "CmdOrCtrl+Z", selector: "undo:" },
                    { label: "Redo", accelerator: "Shift+CmdOrCtrl+Z", selector: "redo:" },
                    { type: "separator" },
                    { label: "Cut", accelerator: "CmdOrCtrl+X", selector: "cut:" },
                    { label: "Copy", accelerator: "CmdOrCtrl+C", selector: "copy:" },
                    { label: "Paste", accelerator: "CmdOrCtrl+V", selector: "paste:" },
                    { label: "Select All", accelerator: "CmdOrCtrl+A", selector: "selectAll:" },
                ]
            }
        ];
        const menu = Electron.Menu.buildFromTemplate(template);
        Electron.Menu.setApplicationMenu(menu);
    }
}
(() => {
    console.log("STARTING THE THING!");
    new ApiModellerApp();
})();
//# sourceMappingURL=main.js.map