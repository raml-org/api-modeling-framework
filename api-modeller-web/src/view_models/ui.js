import { label } from "../utils";
import { NS_MAPPING } from "../main/domain_model";
export class UI {
    iconClassForUnit(unit) {
        if (unit.kind === "Document") {
            return "fa fa-file";
        }
        else if (unit.kind === "Fragment") {
            return "fa fa-puzzle-piece";
        }
        else if (unit.kind === "Module") {
            return "fa fa-archive";
        }
        else {
            return "fa fa-question";
        }
    }
    iconClassForDomainUnit(unit) {
        //console.log("ICON FOR " + unit.id + " => " + unit.elementClass);
        if (unit.elementClass.endsWith("#APIDocumentation")) {
            return "fa fa-book";
        }
        else if (unit.elementClass.endsWith("#Payload")) {
            return "fa fa-paper-plane";
        }
        else if (unit.elementClass.endsWith("#Schema")) {
            return "fa fa-cubes";
        }
        else if (unit.elementClass.endsWith("#Operation")) {
            return "fa fa-rocket";
        }
        else {
            return "fa fa-code";
        }
    }
    iconClassForDomainRDFUnit(unit) {
        if (unit.kind === "APIDocumentation") {
            return "fa fa-book";
        }
        else if (unit.kind === "EndPoint") {
            return "fa fa-link";
        }
        else if (unit.kind === "Operation") {
            return "fa fa-rocket";
        }
        else if (unit.kind === "Request") {
            return "fa fa-envelope";
        }
        else if (unit.kind === "Response") {
            return "fa fa-envelope-open";
        }
        else if (unit.kind === "Payload") {
            return "fa fa-paper-plane";
        }
        else if (unit.kind === "Schema") {
            return "fa fa-cubes";
        }
        else {
            return "fa fa-code";
        }
    }
    labelForDomainUnit(unit) {
        if (unit.label != null) {
            return unit.label;
        }
        else {
            if (unit.kind === "APIDocumentation") {
                return label(unit.id);
            }
            else if (unit.kind === "EndPoint") {
                return unit.path || unit.label || label(unit.id);
            }
            else if (unit.kind === "Operation") {
                return unit.method || unit.label || label(unit.id);
            }
            else if (unit.kind === "Response") {
                return unit.status || unit.label || label(unit.id);
            }
            else if (unit.kind === "Payload") {
                return unit.mediaType || "*/*";
            }
            else if (unit.kind === "Schema") {
                if (unit.label) {
                    return unit.label;
                }
                else if (unit.shape != null) {
                    return label(unit.shape["@id"]);
                }
                else {
                    return label(unit.id);
                }
            }
            else {
                return unit.label || label(unit.id);
            }
        }
    }
    bindingLabel(binding) {
        if (binding) {
            if (binding.token === "uri") {
                for (let prefix in NS_MAPPING) {
                    if (binding.value.startsWith(prefix)) {
                        const rest = binding.value.replace(prefix, "");
                        return NS_MAPPING[prefix] + ":" + rest;
                    }
                }
                return label(binding.value);
            }
            else {
                return binding.value;
            }
        }
        else {
            return "";
        }
    }
}
//# sourceMappingURL=ui.js.map