import * as units from "../main/units_model";
import * as domain from "../main/domain_model";
import {label} from "../utils";
import {Operation, EndPoint} from "../main/domain_model";
import {Response} from "../main/domain_model";

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
        if (unit.elementClass.endsWith("#APIDocumentation")) {
            return "fa fa-book"
        } else if (unit.elementClass.endsWith("#Payload")) {
            return "fa fa-cubes"
        } else if (unit.elementClass.endsWith("#Operation")) {
            return "fa fa-rocket"
        } else {
            return "fa fa-code"
        }
    }

    iconClassForDomainRDFUnit(unit: domain.DomainElement) {
        if (unit.kind === "APIDocumentation") {
            return "fa fa-book"
        } else if (unit.kind === "EndPoint") {
            return "fa fa-link"
        } else if (unit.kind === "Operation") {
            return "fa fa-rocket"
        } else if (unit.kind === "Response") {
            return "fa fa-envelope-open"
        } else if (unit.kind === "Payload") {
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
                return (unit as EndPoint).path || label(unit.id);
            } else if (unit.kind === "Operation") {
                return (unit as Operation).method || label(unit.id)
            } else if (unit.kind === "Response") {
                return (unit as Response).status || label(unit.id)
            } else if (unit.kind === "Payload") {
                return label(unit.id);
            } else {
                return label(unit.id)
            }
        }
    }
}