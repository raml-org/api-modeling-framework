import * as units from "../main/units_model";
import * as domain from "../main/domain_model";
import {label} from "../utils";
import {Operation, EndPoint} from "../main/domain_model";
import {Response} from "../main/domain_model";
import {RdfValue} from "rdfstore";
import {NS_MAPPING} from "../main/domain_model";
import {Payload} from "../main/domain_model";
import {Schema} from "../main/domain_model";

const endsWith = (s: string, postfix: string): boolean => {
    const postIndex = s.indexOf(postfix);
    return s.length == postIndex + postfix.length;
};

export class UI {
    iconClassForUnit(unit: units.DocumentId) {
        if (unit.kind === "Document") {
            return "fa fa-file";
        } else if (unit.kind === "Fragment") {
            return "fa fa-puzzle-piece";
        } else if (unit.kind === "Module") {
            return "fa fa-archive";
        } else {
            return "fa fa-question";
        }
    }

    iconClassForDomainUnit(unit: units.DomainElement) {
        //console.log("ICON FOR " + unit.id + " => " + unit.elementClass);
        if (endsWith(unit.elementClass, "#APIDocumentation")) {
            return "fa fa-book";
        } else if (endsWith(unit.elementClass, "#Payload")) {
            return "fa fa-paper-plane";
        } else if (endsWith(unit.elementClass, "#Schema")) {
            return "fa fa-cubes";
        } else if (endsWith(unit.elementClass, "#Operation")) {
            return "fa fa-rocket";
        } else {
            return "fa fa-code";
        }
    }

    iconClassForDomainRDFUnit(unit: domain.DomainElement) {
        if (unit.kind === "APIDocumentation") {
            return "fa fa-book"
        } else if (unit.kind === "EndPoint") {
            return "fa fa-link"
        } else if (unit.kind === "Operation") {
            return "fa fa-rocket"
        } else if (unit.kind === "Request") {
            return "fa fa-envelope"
        } else if (unit.kind === "Response") {
            return "fa fa-envelope-open"
        } else if (unit.kind === "Payload") {
            return "fa fa-paper-plane"
        } else if (unit.kind === "Schema") {
            return "fa fa-cubes"
        } else {
            return "fa fa-code"
        }
    }

    labelForDomainUnit(unit: domain.DomainElement) {
        if (unit.label != null) {
            return unit.label;
        } else {
            if (unit.kind === "APIDocumentation") {
                return label(unit.id)
            } else if (unit.kind === "EndPoint") {
                return (unit as EndPoint).path || unit.label || label(unit.id);
            } else if (unit.kind === "Operation") {
                return (unit as Operation).method || unit.label || label(unit.id)
            } else if (unit.kind === "Response") {
                return (unit as Response).status || unit.label || label(unit.id)
            } else if (unit.kind === "Payload") {
                return (unit as Payload).mediaType || "*/*";
            } else if (unit.kind === "Schema") {
                if (unit.label) {
                    return unit.label;
                } else if ((unit as Schema).shape != null) {
                    return label((unit as Schema).shape["@id"]);
                } else {
                    return label(unit.id);
                }
            } else {
                return unit.label || label(unit.id)
            }
        }
    }

    bindingLabel(binding: RdfValue) {
        if (binding) {
            if (binding.token === "uri") {
                for (let prefix in NS_MAPPING) {
                    if (binding.value.startsWith(prefix)) {
                        const rest = binding.value.replace(prefix, "");
                        return NS_MAPPING[prefix] + ":" + rest;
                    }
                }
                return label(binding.value);
            } else {
                return binding.value;
            }
        } else {
            return "";
        }
    }
}