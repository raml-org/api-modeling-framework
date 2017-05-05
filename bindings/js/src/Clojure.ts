/**
 * Created by antoniogarrote on 05/05/2017.
 */

require("api-modeling-framework");
const cljscore = global['cljs'].core;

export class Clojure {
    public static kw(s: string): any {
        return cljscore.keyword(s);
    }

    public static cljsToJs(o: any): any {
        return cljscore.clj__GT_js(o);
    }

    public static jsToCljs(o: any): any {
        return cljscore.js__GT_clj(o);
    }

    public static cljsMap<T>(xs: any, f:(x: any) => T): T[] {
        const acc: T[] = [];
        while(! cljscore.empty_QMARK_(xs)) {
            const first = cljscore.first(xs);
            xs = cljscore.rest(xs);
            acc.push(f(first));
        }

        return acc;
    }

    public static toCljsSeq(xs: any[]): any {
        let acc = cljscore.vec();
        xs.forEach(x => {
            acc = cljscore.cons(x, acc);
        });

        return cljscore.reverse(acc);
    }

    public static core = global['cljs'].core;
    public static amf = global['api_modeling_framework'].core;
    public static amf_document = global['api_modeling_framework'].model.document;
    public static amf_domain = global['api_modeling_framework'].model.domain;
}