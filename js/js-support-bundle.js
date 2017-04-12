(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.yaml = f()}})(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
'use strict';


var yaml = require('./lib/js-yaml.js');


module.exports = yaml;

},{"./lib/js-yaml.js":2}],2:[function(require,module,exports){
'use strict';


var loader = require('./js-yaml/loader');
var dumper = require('./js-yaml/dumper');


function deprecated(name) {
  return function () {
    throw new Error('Function ' + name + ' is deprecated and cannot be used.');
  };
}


module.exports.Type                = require('./js-yaml/type');
module.exports.Schema              = require('./js-yaml/schema');
module.exports.FAILSAFE_SCHEMA     = require('./js-yaml/schema/failsafe');
module.exports.JSON_SCHEMA         = require('./js-yaml/schema/json');
module.exports.CORE_SCHEMA         = require('./js-yaml/schema/core');
module.exports.DEFAULT_SAFE_SCHEMA = require('./js-yaml/schema/default_safe');
module.exports.DEFAULT_FULL_SCHEMA = require('./js-yaml/schema/default_full');
module.exports.load                = loader.load;
module.exports.loadAll             = loader.loadAll;
module.exports.safeLoad            = loader.safeLoad;
module.exports.safeLoadAll         = loader.safeLoadAll;
module.exports.dump                = dumper.dump;
module.exports.safeDump            = dumper.safeDump;
module.exports.YAMLException       = require('./js-yaml/exception');

// Deprecated schema names from JS-YAML 2.0.x
module.exports.MINIMAL_SCHEMA = require('./js-yaml/schema/failsafe');
module.exports.SAFE_SCHEMA    = require('./js-yaml/schema/default_safe');
module.exports.DEFAULT_SCHEMA = require('./js-yaml/schema/default_full');

// Deprecated functions from JS-YAML 1.x.x
module.exports.scan           = deprecated('scan');
module.exports.parse          = deprecated('parse');
module.exports.compose        = deprecated('compose');
module.exports.addConstructor = deprecated('addConstructor');

},{"./js-yaml/dumper":4,"./js-yaml/exception":5,"./js-yaml/loader":6,"./js-yaml/schema":8,"./js-yaml/schema/core":9,"./js-yaml/schema/default_full":10,"./js-yaml/schema/default_safe":11,"./js-yaml/schema/failsafe":12,"./js-yaml/schema/json":13,"./js-yaml/type":14}],3:[function(require,module,exports){
'use strict';


function isNothing(subject) {
  return (typeof subject === 'undefined') || (subject === null);
}


function isObject(subject) {
  return (typeof subject === 'object') && (subject !== null);
}


function toArray(sequence) {
  if (Array.isArray(sequence)) return sequence;
  else if (isNothing(sequence)) return [];

  return [ sequence ];
}


function extend(target, source) {
  var index, length, key, sourceKeys;

  if (source) {
    sourceKeys = Object.keys(source);

    for (index = 0, length = sourceKeys.length; index < length; index += 1) {
      key = sourceKeys[index];
      target[key] = source[key];
    }
  }

  return target;
}


function repeat(string, count) {
  var result = '', cycle;

  for (cycle = 0; cycle < count; cycle += 1) {
    result += string;
  }

  return result;
}


function isNegativeZero(number) {
  return (number === 0) && (Number.NEGATIVE_INFINITY === 1 / number);
}


module.exports.isNothing      = isNothing;
module.exports.isObject       = isObject;
module.exports.toArray        = toArray;
module.exports.repeat         = repeat;
module.exports.isNegativeZero = isNegativeZero;
module.exports.extend         = extend;

},{}],4:[function(require,module,exports){
'use strict';

/*eslint-disable no-use-before-define*/

var common              = require('./common');
var YAMLException       = require('./exception');
var DEFAULT_FULL_SCHEMA = require('./schema/default_full');
var DEFAULT_SAFE_SCHEMA = require('./schema/default_safe');

var _toString       = Object.prototype.toString;
var _hasOwnProperty = Object.prototype.hasOwnProperty;

var CHAR_TAB                  = 0x09; /* Tab */
var CHAR_LINE_FEED            = 0x0A; /* LF */
var CHAR_SPACE                = 0x20; /* Space */
var CHAR_EXCLAMATION          = 0x21; /* ! */
var CHAR_DOUBLE_QUOTE         = 0x22; /* " */
var CHAR_SHARP                = 0x23; /* # */
var CHAR_PERCENT              = 0x25; /* % */
var CHAR_AMPERSAND            = 0x26; /* & */
var CHAR_SINGLE_QUOTE         = 0x27; /* ' */
var CHAR_ASTERISK             = 0x2A; /* * */
var CHAR_COMMA                = 0x2C; /* , */
var CHAR_MINUS                = 0x2D; /* - */
var CHAR_COLON                = 0x3A; /* : */
var CHAR_GREATER_THAN         = 0x3E; /* > */
var CHAR_QUESTION             = 0x3F; /* ? */
var CHAR_COMMERCIAL_AT        = 0x40; /* @ */
var CHAR_LEFT_SQUARE_BRACKET  = 0x5B; /* [ */
var CHAR_RIGHT_SQUARE_BRACKET = 0x5D; /* ] */
var CHAR_GRAVE_ACCENT         = 0x60; /* ` */
var CHAR_LEFT_CURLY_BRACKET   = 0x7B; /* { */
var CHAR_VERTICAL_LINE        = 0x7C; /* | */
var CHAR_RIGHT_CURLY_BRACKET  = 0x7D; /* } */

var ESCAPE_SEQUENCES = {};

ESCAPE_SEQUENCES[0x00]   = '\\0';
ESCAPE_SEQUENCES[0x07]   = '\\a';
ESCAPE_SEQUENCES[0x08]   = '\\b';
ESCAPE_SEQUENCES[0x09]   = '\\t';
ESCAPE_SEQUENCES[0x0A]   = '\\n';
ESCAPE_SEQUENCES[0x0B]   = '\\v';
ESCAPE_SEQUENCES[0x0C]   = '\\f';
ESCAPE_SEQUENCES[0x0D]   = '\\r';
ESCAPE_SEQUENCES[0x1B]   = '\\e';
ESCAPE_SEQUENCES[0x22]   = '\\"';
ESCAPE_SEQUENCES[0x5C]   = '\\\\';
ESCAPE_SEQUENCES[0x85]   = '\\N';
ESCAPE_SEQUENCES[0xA0]   = '\\_';
ESCAPE_SEQUENCES[0x2028] = '\\L';
ESCAPE_SEQUENCES[0x2029] = '\\P';

var DEPRECATED_BOOLEANS_SYNTAX = [
  'y', 'Y', 'yes', 'Yes', 'YES', 'on', 'On', 'ON',
  'n', 'N', 'no', 'No', 'NO', 'off', 'Off', 'OFF'
];

function compileStyleMap(schema, map) {
  var result, keys, index, length, tag, style, type;

  if (map === null) return {};

  result = {};
  keys = Object.keys(map);

  for (index = 0, length = keys.length; index < length; index += 1) {
    tag = keys[index];
    style = String(map[tag]);

    if (tag.slice(0, 2) === '!!') {
      tag = 'tag:yaml.org,2002:' + tag.slice(2);
    }
    type = schema.compiledTypeMap['fallback'][tag];

    if (type && _hasOwnProperty.call(type.styleAliases, style)) {
      style = type.styleAliases[style];
    }

    result[tag] = style;
  }

  return result;
}

function encodeHex(character) {
  var string, handle, length;

  string = character.toString(16).toUpperCase();

  if (character <= 0xFF) {
    handle = 'x';
    length = 2;
  } else if (character <= 0xFFFF) {
    handle = 'u';
    length = 4;
  } else if (character <= 0xFFFFFFFF) {
    handle = 'U';
    length = 8;
  } else {
    throw new YAMLException('code point within a string may not be greater than 0xFFFFFFFF');
  }

  return '\\' + handle + common.repeat('0', length - string.length) + string;
}

function State(options) {
  this.schema       = options['schema'] || DEFAULT_FULL_SCHEMA;
  this.indent       = Math.max(1, (options['indent'] || 2));
  this.skipInvalid  = options['skipInvalid'] || false;
  this.flowLevel    = (common.isNothing(options['flowLevel']) ? -1 : options['flowLevel']);
  this.styleMap     = compileStyleMap(this.schema, options['styles'] || null);
  this.sortKeys     = options['sortKeys'] || false;
  this.lineWidth    = options['lineWidth'] || 80;
  this.noRefs       = options['noRefs'] || false;
  this.noCompatMode = options['noCompatMode'] || false;

  this.implicitTypes = this.schema.compiledImplicit;
  this.explicitTypes = this.schema.compiledExplicit;

  this.tag = null;
  this.result = '';

  this.duplicates = [];
  this.usedDuplicates = null;
}

// Indents every line in a string. Empty lines (\n only) are not indented.
function indentString(string, spaces) {
  var ind = common.repeat(' ', spaces),
      position = 0,
      next = -1,
      result = '',
      line,
      length = string.length;

  while (position < length) {
    next = string.indexOf('\n', position);
    if (next === -1) {
      line = string.slice(position);
      position = length;
    } else {
      line = string.slice(position, next + 1);
      position = next + 1;
    }

    if (line.length && line !== '\n') result += ind;

    result += line;
  }

  return result;
}

function generateNextLine(state, level) {
  return '\n' + common.repeat(' ', state.indent * level);
}

function testImplicitResolving(state, str) {
  var index, length, type;

  for (index = 0, length = state.implicitTypes.length; index < length; index += 1) {
    type = state.implicitTypes[index];

    if (type.resolve(str)) {
      return true;
    }
  }

  return false;
}

// [33] s-white ::= s-space | s-tab
function isWhitespace(c) {
  return c === CHAR_SPACE || c === CHAR_TAB;
}

// Returns true if the character can be printed without escaping.
// From YAML 1.2: "any allowed characters known to be non-printable
// should also be escaped. [However,] This isn’t mandatory"
// Derived from nb-char - \t - #x85 - #xA0 - #x2028 - #x2029.
function isPrintable(c) {
  return  (0x00020 <= c && c <= 0x00007E)
      || ((0x000A1 <= c && c <= 0x00D7FF) && c !== 0x2028 && c !== 0x2029)
      || ((0x0E000 <= c && c <= 0x00FFFD) && c !== 0xFEFF /* BOM */)
      ||  (0x10000 <= c && c <= 0x10FFFF);
}

// Simplified test for values allowed after the first character in plain style.
function isPlainSafe(c) {
  // Uses a subset of nb-char - c-flow-indicator - ":" - "#"
  // where nb-char ::= c-printable - b-char - c-byte-order-mark.
  return isPrintable(c) && c !== 0xFEFF
    // - c-flow-indicator
    && c !== CHAR_COMMA
    && c !== CHAR_LEFT_SQUARE_BRACKET
    && c !== CHAR_RIGHT_SQUARE_BRACKET
    && c !== CHAR_LEFT_CURLY_BRACKET
    && c !== CHAR_RIGHT_CURLY_BRACKET
    // - ":" - "#"
    && c !== CHAR_COLON
    && c !== CHAR_SHARP;
}

// Simplified test for values allowed as the first character in plain style.
function isPlainSafeFirst(c) {
  // Uses a subset of ns-char - c-indicator
  // where ns-char = nb-char - s-white.
  return isPrintable(c) && c !== 0xFEFF
    && !isWhitespace(c) // - s-white
    // - (c-indicator ::=
    // “-” | “?” | “:” | “,” | “[” | “]” | “{” | “}”
    && c !== CHAR_MINUS
    && c !== CHAR_QUESTION
    && c !== CHAR_COLON
    && c !== CHAR_COMMA
    && c !== CHAR_LEFT_SQUARE_BRACKET
    && c !== CHAR_RIGHT_SQUARE_BRACKET
    && c !== CHAR_LEFT_CURLY_BRACKET
    && c !== CHAR_RIGHT_CURLY_BRACKET
    // | “#” | “&” | “*” | “!” | “|” | “>” | “'” | “"”
    && c !== CHAR_SHARP
    && c !== CHAR_AMPERSAND
    && c !== CHAR_ASTERISK
    && c !== CHAR_EXCLAMATION
    && c !== CHAR_VERTICAL_LINE
    && c !== CHAR_GREATER_THAN
    && c !== CHAR_SINGLE_QUOTE
    && c !== CHAR_DOUBLE_QUOTE
    // | “%” | “@” | “`”)
    && c !== CHAR_PERCENT
    && c !== CHAR_COMMERCIAL_AT
    && c !== CHAR_GRAVE_ACCENT;
}

var STYLE_PLAIN   = 1,
    STYLE_SINGLE  = 2,
    STYLE_LITERAL = 3,
    STYLE_FOLDED  = 4,
    STYLE_DOUBLE  = 5;

// Determines which scalar styles are possible and returns the preferred style.
// lineWidth = -1 => no limit.
// Pre-conditions: str.length > 0.
// Post-conditions:
//    STYLE_PLAIN or STYLE_SINGLE => no \n are in the string.
//    STYLE_LITERAL => no lines are suitable for folding (or lineWidth is -1).
//    STYLE_FOLDED => a line > lineWidth and can be folded (and lineWidth != -1).
function chooseScalarStyle(string, singleLineOnly, indentPerLevel, lineWidth, testAmbiguousType) {
  var i;
  var char;
  var hasLineBreak = false;
  var hasFoldableLine = false; // only checked if shouldTrackWidth
  var shouldTrackWidth = lineWidth !== -1;
  var previousLineBreak = -1; // count the first line correctly
  var plain = isPlainSafeFirst(string.charCodeAt(0))
          && !isWhitespace(string.charCodeAt(string.length - 1));

  if (singleLineOnly) {
    // Case: no block styles.
    // Check for disallowed characters to rule out plain and single.
    for (i = 0; i < string.length; i++) {
      char = string.charCodeAt(i);
      if (!isPrintable(char)) {
        return STYLE_DOUBLE;
      }
      plain = plain && isPlainSafe(char);
    }
  } else {
    // Case: block styles permitted.
    for (i = 0; i < string.length; i++) {
      char = string.charCodeAt(i);
      if (char === CHAR_LINE_FEED) {
        hasLineBreak = true;
        // Check if any line can be folded.
        if (shouldTrackWidth) {
          hasFoldableLine = hasFoldableLine ||
            // Foldable line = too long, and not more-indented.
            (i - previousLineBreak - 1 > lineWidth &&
             string[previousLineBreak + 1] !== ' ');
          previousLineBreak = i;
        }
      } else if (!isPrintable(char)) {
        return STYLE_DOUBLE;
      }
      plain = plain && isPlainSafe(char);
    }
    // in case the end is missing a \n
    hasFoldableLine = hasFoldableLine || (shouldTrackWidth &&
      (i - previousLineBreak - 1 > lineWidth &&
       string[previousLineBreak + 1] !== ' '));
  }
  // Although every style can represent \n without escaping, prefer block styles
  // for multiline, since they're more readable and they don't add empty lines.
  // Also prefer folding a super-long line.
  if (!hasLineBreak && !hasFoldableLine) {
    // Strings interpretable as another type have to be quoted;
    // e.g. the string 'true' vs. the boolean true.
    return plain && !testAmbiguousType(string)
      ? STYLE_PLAIN : STYLE_SINGLE;
  }
  // Edge case: block indentation indicator can only have one digit.
  if (string[0] === ' ' && indentPerLevel > 9) {
    return STYLE_DOUBLE;
  }
  // At this point we know block styles are valid.
  // Prefer literal style unless we want to fold.
  return hasFoldableLine ? STYLE_FOLDED : STYLE_LITERAL;
}

// Note: line breaking/folding is implemented for only the folded style.
// NB. We drop the last trailing newline (if any) of a returned block scalar
//  since the dumper adds its own newline. This always works:
//    • No ending newline => unaffected; already using strip "-" chomping.
//    • Ending newline    => removed then restored.
//  Importantly, this keeps the "+" chomp indicator from gaining an extra line.
function writeScalar(state, string, level, iskey) {
  state.dump = (function () {
    if (string.length === 0) {
      return "''";
    }
    if (!state.noCompatMode &&
        DEPRECATED_BOOLEANS_SYNTAX.indexOf(string) !== -1) {
      return "'" + string + "'";
    }

    var indent = state.indent * Math.max(1, level); // no 0-indent scalars
    // As indentation gets deeper, let the width decrease monotonically
    // to the lower bound min(state.lineWidth, 40).
    // Note that this implies
    //  state.lineWidth ≤ 40 + state.indent: width is fixed at the lower bound.
    //  state.lineWidth > 40 + state.indent: width decreases until the lower bound.
    // This behaves better than a constant minimum width which disallows narrower options,
    // or an indent threshold which causes the width to suddenly increase.
    var lineWidth = state.lineWidth === -1
      ? -1 : Math.max(Math.min(state.lineWidth, 40), state.lineWidth - indent);

    // Without knowing if keys are implicit/explicit, assume implicit for safety.
    var singleLineOnly = iskey
      // No block styles in flow mode.
      || (state.flowLevel > -1 && level >= state.flowLevel);
    function testAmbiguity(string) {
      return testImplicitResolving(state, string);
    }

    switch (chooseScalarStyle(string, singleLineOnly, state.indent, lineWidth, testAmbiguity)) {
      case STYLE_PLAIN:
        return string;
      case STYLE_SINGLE:
        return "'" + string.replace(/'/g, "''") + "'";
      case STYLE_LITERAL:
        return '|' + blockHeader(string, state.indent)
          + dropEndingNewline(indentString(string, indent));
      case STYLE_FOLDED:
        return '>' + blockHeader(string, state.indent)
          + dropEndingNewline(indentString(foldString(string, lineWidth), indent));
      case STYLE_DOUBLE:
        return '"' + escapeString(string, lineWidth) + '"';
      default:
        throw new YAMLException('impossible error: invalid scalar style');
    }
  }());
}

// Pre-conditions: string is valid for a block scalar, 1 <= indentPerLevel <= 9.
function blockHeader(string, indentPerLevel) {
  var indentIndicator = (string[0] === ' ') ? String(indentPerLevel) : '';

  // note the special case: the string '\n' counts as a "trailing" empty line.
  var clip =          string[string.length - 1] === '\n';
  var keep = clip && (string[string.length - 2] === '\n' || string === '\n');
  var chomp = keep ? '+' : (clip ? '' : '-');

  return indentIndicator + chomp + '\n';
}

// (See the note for writeScalar.)
function dropEndingNewline(string) {
  return string[string.length - 1] === '\n' ? string.slice(0, -1) : string;
}

// Note: a long line without a suitable break point will exceed the width limit.
// Pre-conditions: every char in str isPrintable, str.length > 0, width > 0.
function foldString(string, width) {
  // In folded style, $k$ consecutive newlines output as $k+1$ newlines—
  // unless they're before or after a more-indented line, or at the very
  // beginning or end, in which case $k$ maps to $k$.
  // Therefore, parse each chunk as newline(s) followed by a content line.
  var lineRe = /(\n+)([^\n]*)/g;

  // first line (possibly an empty line)
  var result = (function () {
    var nextLF = string.indexOf('\n');
    nextLF = nextLF !== -1 ? nextLF : string.length;
    lineRe.lastIndex = nextLF;
    return foldLine(string.slice(0, nextLF), width);
  }());
  // If we haven't reached the first content line yet, don't add an extra \n.
  var prevMoreIndented = string[0] === '\n' || string[0] === ' ';
  var moreIndented;

  // rest of the lines
  var match;
  while ((match = lineRe.exec(string))) {
    var prefix = match[1], line = match[2];
    moreIndented = (line[0] === ' ');
    result += prefix
      + (!prevMoreIndented && !moreIndented && line !== ''
        ? '\n' : '')
      + foldLine(line, width);
    prevMoreIndented = moreIndented;
  }

  return result;
}

// Greedy line breaking.
// Picks the longest line under the limit each time,
// otherwise settles for the shortest line over the limit.
// NB. More-indented lines *cannot* be folded, as that would add an extra \n.
function foldLine(line, width) {
  if (line === '' || line[0] === ' ') return line;

  // Since a more-indented line adds a \n, breaks can't be followed by a space.
  var breakRe = / [^ ]/g; // note: the match index will always be <= length-2.
  var match;
  // start is an inclusive index. end, curr, and next are exclusive.
  var start = 0, end, curr = 0, next = 0;
  var result = '';

  // Invariants: 0 <= start <= length-1.
  //   0 <= curr <= next <= max(0, length-2). curr - start <= width.
  // Inside the loop:
  //   A match implies length >= 2, so curr and next are <= length-2.
  while ((match = breakRe.exec(line))) {
    next = match.index;
    // maintain invariant: curr - start <= width
    if (next - start > width) {
      end = (curr > start) ? curr : next; // derive end <= length-2
      result += '\n' + line.slice(start, end);
      // skip the space that was output as \n
      start = end + 1;                    // derive start <= length-1
    }
    curr = next;
  }

  // By the invariants, start <= length-1, so there is something left over.
  // It is either the whole string or a part starting from non-whitespace.
  result += '\n';
  // Insert a break if the remainder is too long and there is a break available.
  if (line.length - start > width && curr > start) {
    result += line.slice(start, curr) + '\n' + line.slice(curr + 1);
  } else {
    result += line.slice(start);
  }

  return result.slice(1); // drop extra \n joiner
}

// Escapes a double-quoted string.
function escapeString(string) {
  var result = '';
  var char;
  var escapeSeq;

  for (var i = 0; i < string.length; i++) {
    char = string.charCodeAt(i);
    escapeSeq = ESCAPE_SEQUENCES[char];
    result += !escapeSeq && isPrintable(char)
      ? string[i]
      : escapeSeq || encodeHex(char);
  }

  return result;
}

function writeFlowSequence(state, level, object) {
  var _result = '',
      _tag    = state.tag,
      index,
      length;

  for (index = 0, length = object.length; index < length; index += 1) {
    // Write only valid elements.
    if (writeNode(state, level, object[index], false, false)) {
      if (index !== 0) _result += ', ';
      _result += state.dump;
    }
  }

  state.tag = _tag;
  state.dump = '[' + _result + ']';
}

function writeBlockSequence(state, level, object, compact) {
  var _result = '',
      _tag    = state.tag,
      index,
      length;

  for (index = 0, length = object.length; index < length; index += 1) {
    // Write only valid elements.
    if (writeNode(state, level + 1, object[index], true, true)) {
      if (!compact || index !== 0) {
        _result += generateNextLine(state, level);
      }
      _result += '- ' + state.dump;
    }
  }

  state.tag = _tag;
  state.dump = _result || '[]'; // Empty sequence if no valid values.
}

function writeFlowMapping(state, level, object) {
  var _result       = '',
      _tag          = state.tag,
      objectKeyList = Object.keys(object),
      index,
      length,
      objectKey,
      objectValue,
      pairBuffer;

  for (index = 0, length = objectKeyList.length; index < length; index += 1) {
    pairBuffer = '';

    if (index !== 0) pairBuffer += ', ';

    objectKey = objectKeyList[index];
    objectValue = object[objectKey];

    if (!writeNode(state, level, objectKey, false, false)) {
      continue; // Skip this pair because of invalid key;
    }

    if (state.dump.length > 1024) pairBuffer += '? ';

    pairBuffer += state.dump + ': ';

    if (!writeNode(state, level, objectValue, false, false)) {
      continue; // Skip this pair because of invalid value.
    }

    pairBuffer += state.dump;

    // Both key and value are valid.
    _result += pairBuffer;
  }

  state.tag = _tag;
  state.dump = '{' + _result + '}';
}

function writeBlockMapping(state, level, object, compact) {
  var _result       = '',
      _tag          = state.tag,
      objectKeyList = Object.keys(object),
      index,
      length,
      objectKey,
      objectValue,
      explicitPair,
      pairBuffer;

  // Allow sorting keys so that the output file is deterministic
  if (state.sortKeys === true) {
    // Default sorting
    objectKeyList.sort();
  } else if (typeof state.sortKeys === 'function') {
    // Custom sort function
    objectKeyList.sort(state.sortKeys);
  } else if (state.sortKeys) {
    // Something is wrong
    throw new YAMLException('sortKeys must be a boolean or a function');
  }

  for (index = 0, length = objectKeyList.length; index < length; index += 1) {
    pairBuffer = '';

    if (!compact || index !== 0) {
      pairBuffer += generateNextLine(state, level);
    }

    objectKey = objectKeyList[index];
    objectValue = object[objectKey];

    if (!writeNode(state, level + 1, objectKey, true, true, true)) {
      continue; // Skip this pair because of invalid key.
    }

    explicitPair = (state.tag !== null && state.tag !== '?') ||
                   (state.dump && state.dump.length > 1024);

    if (explicitPair) {
      if (state.dump && CHAR_LINE_FEED === state.dump.charCodeAt(0)) {
        pairBuffer += '?';
      } else {
        pairBuffer += '? ';
      }
    }

    pairBuffer += state.dump;

    if (explicitPair) {
      pairBuffer += generateNextLine(state, level);
    }

    if (!writeNode(state, level + 1, objectValue, true, explicitPair)) {
      continue; // Skip this pair because of invalid value.
    }

    if (state.dump && CHAR_LINE_FEED === state.dump.charCodeAt(0)) {
      pairBuffer += ':';
    } else {
      pairBuffer += ': ';
    }

    pairBuffer += state.dump;

    // Both key and value are valid.
    _result += pairBuffer;
  }

  state.tag = _tag;
  state.dump = _result || '{}'; // Empty mapping if no valid pairs.
}

function detectType(state, object, explicit) {
  var _result, typeList, index, length, type, style;

  typeList = explicit ? state.explicitTypes : state.implicitTypes;

  for (index = 0, length = typeList.length; index < length; index += 1) {
    type = typeList[index];

    if ((type.instanceOf  || type.predicate) &&
        (!type.instanceOf || ((typeof object === 'object') && (object instanceof type.instanceOf))) &&
        (!type.predicate  || type.predicate(object))) {

      state.tag = explicit ? type.tag : '?';

      if (type.represent) {
        style = state.styleMap[type.tag] || type.defaultStyle;

        if (_toString.call(type.represent) === '[object Function]') {
          _result = type.represent(object, style);
        } else if (_hasOwnProperty.call(type.represent, style)) {
          _result = type.represent[style](object, style);
        } else {
          throw new YAMLException('!<' + type.tag + '> tag resolver accepts not "' + style + '" style');
        }

        state.dump = _result;
      }

      return true;
    }
  }

  return false;
}

// Serializes `object` and writes it to global `result`.
// Returns true on success, or false on invalid object.
//
function writeNode(state, level, object, block, compact, iskey) {
  state.tag = null;
  state.dump = object;

  if (!detectType(state, object, false)) {
    detectType(state, object, true);
  }

  var type = _toString.call(state.dump);

  if (block) {
    block = (state.flowLevel < 0 || state.flowLevel > level);
  }

  var objectOrArray = type === '[object Object]' || type === '[object Array]',
      duplicateIndex,
      duplicate;

  if (objectOrArray) {
    duplicateIndex = state.duplicates.indexOf(object);
    duplicate = duplicateIndex !== -1;
  }

  if ((state.tag !== null && state.tag !== '?') || duplicate || (state.indent !== 2 && level > 0)) {
    compact = false;
  }

  if (duplicate && state.usedDuplicates[duplicateIndex]) {
    state.dump = '*ref_' + duplicateIndex;
  } else {
    if (objectOrArray && duplicate && !state.usedDuplicates[duplicateIndex]) {
      state.usedDuplicates[duplicateIndex] = true;
    }
    if (type === '[object Object]') {
      if (block && (Object.keys(state.dump).length !== 0)) {
        writeBlockMapping(state, level, state.dump, compact);
        if (duplicate) {
          state.dump = '&ref_' + duplicateIndex + state.dump;
        }
      } else {
        writeFlowMapping(state, level, state.dump);
        if (duplicate) {
          state.dump = '&ref_' + duplicateIndex + ' ' + state.dump;
        }
      }
    } else if (type === '[object Array]') {
      if (block && (state.dump.length !== 0)) {
        writeBlockSequence(state, level, state.dump, compact);
        if (duplicate) {
          state.dump = '&ref_' + duplicateIndex + state.dump;
        }
      } else {
        writeFlowSequence(state, level, state.dump);
        if (duplicate) {
          state.dump = '&ref_' + duplicateIndex + ' ' + state.dump;
        }
      }
    } else if (type === '[object String]') {
      if (state.tag !== '?') {
        writeScalar(state, state.dump, level, iskey);
      }
    } else {
      if (state.skipInvalid) return false;
      throw new YAMLException('unacceptable kind of an object to dump ' + type);
    }

    if (state.tag !== null && state.tag !== '?') {
      state.dump = '!<' + state.tag + '> ' + state.dump;
    }
  }

  return true;
}

function getDuplicateReferences(object, state) {
  var objects = [],
      duplicatesIndexes = [],
      index,
      length;

  inspectNode(object, objects, duplicatesIndexes);

  for (index = 0, length = duplicatesIndexes.length; index < length; index += 1) {
    state.duplicates.push(objects[duplicatesIndexes[index]]);
  }
  state.usedDuplicates = new Array(length);
}

function inspectNode(object, objects, duplicatesIndexes) {
  var objectKeyList,
      index,
      length;

  if (object !== null && typeof object === 'object') {
    index = objects.indexOf(object);
    if (index !== -1) {
      if (duplicatesIndexes.indexOf(index) === -1) {
        duplicatesIndexes.push(index);
      }
    } else {
      objects.push(object);

      if (Array.isArray(object)) {
        for (index = 0, length = object.length; index < length; index += 1) {
          inspectNode(object[index], objects, duplicatesIndexes);
        }
      } else {
        objectKeyList = Object.keys(object);

        for (index = 0, length = objectKeyList.length; index < length; index += 1) {
          inspectNode(object[objectKeyList[index]], objects, duplicatesIndexes);
        }
      }
    }
  }
}

function dump(input, options) {
  options = options || {};

  var state = new State(options);

  if (!state.noRefs) getDuplicateReferences(input, state);

  if (writeNode(state, 0, input, true, true)) return state.dump + '\n';

  return '';
}

function safeDump(input, options) {
  return dump(input, common.extend({ schema: DEFAULT_SAFE_SCHEMA }, options));
}

module.exports.dump     = dump;
module.exports.safeDump = safeDump;

},{"./common":3,"./exception":5,"./schema/default_full":10,"./schema/default_safe":11}],5:[function(require,module,exports){
// YAML error class. http://stackoverflow.com/questions/8458984
//
'use strict';

function YAMLException(reason, mark) {
  // Super constructor
  Error.call(this);

  // Include stack trace in error object
  if (Error.captureStackTrace) {
    // Chrome and NodeJS
    Error.captureStackTrace(this, this.constructor);
  } else {
    // FF, IE 10+ and Safari 6+. Fallback for others
    this.stack = (new Error()).stack || '';
  }

  this.name = 'YAMLException';
  this.reason = reason;
  this.mark = mark;
  this.message = (this.reason || '(unknown reason)') + (this.mark ? ' ' + this.mark.toString() : '');
}


// Inherit from Error
YAMLException.prototype = Object.create(Error.prototype);
YAMLException.prototype.constructor = YAMLException;


YAMLException.prototype.toString = function toString(compact) {
  var result = this.name + ': ';

  result += this.reason || '(unknown reason)';

  if (!compact && this.mark) {
    result += ' ' + this.mark.toString();
  }

  return result;
};


module.exports = YAMLException;

},{}],6:[function(require,module,exports){
'use strict';

/*eslint-disable max-len,no-use-before-define*/

var common = require('./common');
var YAMLException = require('./exception');
var Mark = require('./mark');
var DEFAULT_SAFE_SCHEMA = require('./schema/default_safe');
var DEFAULT_FULL_SCHEMA = require('./schema/default_full');


var _hasOwnProperty = Object.prototype.hasOwnProperty;


var CONTEXT_FLOW_IN = 1;
var CONTEXT_FLOW_OUT = 2;
var CONTEXT_BLOCK_IN = 3;
var CONTEXT_BLOCK_OUT = 4;


var CHOMPING_CLIP = 1;
var CHOMPING_STRIP = 2;
var CHOMPING_KEEP = 3;


var PATTERN_NON_PRINTABLE = /[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x84\x86-\x9F\uFFFE\uFFFF]|[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?:[^\uD800-\uDBFF]|^)[\uDC00-\uDFFF]/;
var PATTERN_NON_ASCII_LINE_BREAKS = /[\x85\u2028\u2029]/;
var PATTERN_FLOW_INDICATORS = /[,\[\]\{\}]/;
var PATTERN_TAG_HANDLE = /^(?:!|!!|![a-z\-]+!)$/i;
var PATTERN_TAG_URI = /^(?:!|[^,\[\]\{\}])(?:%[0-9a-f]{2}|[0-9a-z\-#;\/\?:@&=\+\$,_\.!~\*'\(\)\[\]])*$/i;


function is_EOL(c) {
    return (c === 0x0A/* LF */) || (c === 0x0D/* CR */);
}

function is_WHITE_SPACE(c) {
    return (c === 0x09/* Tab */) || (c === 0x20/* Space */);
}

function is_WS_OR_EOL(c) {
    return (c === 0x09/* Tab */) ||
        (c === 0x20/* Space */) ||
        (c === 0x0A/* LF */) ||
        (c === 0x0D/* CR */);
}

function is_FLOW_INDICATOR(c) {
    return c === 0x2C/* , */ ||
        c === 0x5B/* [ */ ||
        c === 0x5D/* ] */ ||
        c === 0x7B/* { */ ||
        c === 0x7D/* } */;
}

function fromHexCode(c) {
    var lc;

    if ((0x30/* 0 */ <= c) && (c <= 0x39/* 9 */)) {
        return c - 0x30;
    }

    /*eslint-disable no-bitwise*/
    lc = c | 0x20;

    if ((0x61/* a */ <= lc) && (lc <= 0x66/* f */)) {
        return lc - 0x61 + 10;
    }

    return -1;
}

function escapedHexLen(c) {
    if (c === 0x78/* x */) { return 2; }
    if (c === 0x75/* u */) { return 4; }
    if (c === 0x55/* U */) { return 8; }
    return 0;
}

function fromDecimalCode(c) {
    if ((0x30/* 0 */ <= c) && (c <= 0x39/* 9 */)) {
        return c - 0x30;
    }

    return -1;
}

function simpleEscapeSequence(c) {
    return (c === 0x30/* 0 */) ? '\x00' :
        (c === 0x61/* a */) ? '\x07' :
            (c === 0x62/* b */) ? '\x08' :
                (c === 0x74/* t */) ? '\x09' :
                    (c === 0x09/* Tab */) ? '\x09' :
                        (c === 0x6E/* n */) ? '\x0A' :
                            (c === 0x76/* v */) ? '\x0B' :
                                (c === 0x66/* f */) ? '\x0C' :
                                    (c === 0x72/* r */) ? '\x0D' :
                                        (c === 0x65/* e */) ? '\x1B' :
                                            (c === 0x20/* Space */) ? ' ' :
                                                (c === 0x22/* " */) ? '\x22' :
                                                    (c === 0x2F/* / */) ? '/' :
                                                        (c === 0x5C/* \ */) ? '\x5C' :
                                                            (c === 0x4E/* N */) ? '\x85' :
                                                                (c === 0x5F/* _ */) ? '\xA0' :
                                                                    (c === 0x4C/* L */) ? '\u2028' :
                                                                        (c === 0x50/* P */) ? '\u2029' : '';
}

function charFromCodepoint(c) {
    if (c <= 0xFFFF) {
        return String.fromCharCode(c);
    }
    // Encode UTF-16 surrogate pair
    // https://en.wikipedia.org/wiki/UTF-16#Code_points_U.2B010000_to_U.2B10FFFF
    return String.fromCharCode(((c - 0x010000) >> 10) + 0xD800,
        ((c - 0x010000) & 0x03FF) + 0xDC00);
}

var simpleEscapeCheck = new Array(256); // integer, for fast access
var simpleEscapeMap = new Array(256);
for (var i = 0; i < 256; i++) {
    simpleEscapeCheck[i] = simpleEscapeSequence(i) ? 1 : 0;
    simpleEscapeMap[i] = simpleEscapeSequence(i);
}


function State(input, options) {
    this.input = input;

    this.filename = options['filename'] || null;
    this.schema = options['schema'] || DEFAULT_FULL_SCHEMA;
    this.onWarning = options['onWarning'] || null;
    this.legacy = options['legacy'] || false;
    this.json = options['json'] || false;
    this.listener = options['listener'] || null;

    this.implicitTypes = this.schema.compiledImplicit;
    this.typeMap = this.schema.compiledTypeMap;

    this.length = input.length;
    this.position = 0;
    this.line = 0;
    this.lineStart = 0;
    this.lineIndent = 0;

    this.documents = [];

    /*
    this.version;
    this.checkLineBreaks;
    this.tagMap;
    this.anchorMap;
    this.tag;
    this.anchor;
    this.kind;
    this.result;*/

}


function generateError(state, message) {
    console.log("ERROR: " + message);
    return new YAMLException(
        message,
        new Mark(state.filename, state.input, state.position, state.line, (state.position - state.lineStart)));
}

function throwError(state, message) {
    throw generateError(state, message);
}

function throwWarning(state, message) {
    if (state.onWarning) {
        state.onWarning.call(null, generateError(state, message));
    }
}


var directiveHandlers = {

    YAML: function handleYamlDirective(state, name, args) {

        var match, major, minor;

        if (state.version !== null) {
            throwError(state, 'duplication of %YAML directive');
        }

        if (args.length !== 1) {
            throwError(state, 'YAML directive accepts exactly one argument');
        }

        match = /^([0-9]+)\.([0-9]+)$/.exec(args[0]);

        if (match === null) {
            throwError(state, 'ill-formed argument of the YAML directive');
        }

        major = parseInt(match[1], 10);
        minor = parseInt(match[2], 10);

        if (major !== 1) {
            throwError(state, 'unacceptable YAML version of the document');
        }

        state.version = args[0];
        state.checkLineBreaks = (minor < 2);

        if (minor !== 1 && minor !== 2) {
            throwWarning(state, 'unsupported YAML version of the document');
        }
    },

    TAG: function handleTagDirective(state, name, args) {

        var handle, prefix;

        if (args.length !== 2) {
            throwError(state, 'TAG directive accepts exactly two arguments');
        }

        handle = args[0];
        prefix = args[1];

        if (!PATTERN_TAG_HANDLE.test(handle)) {
            throwError(state, 'ill-formed tag handle (first argument) of the TAG directive');
        }

        if (_hasOwnProperty.call(state.tagMap, handle)) {
            throwError(state, 'there is a previously declared suffix for "' + handle + '" tag handle');
        }

        if (!PATTERN_TAG_URI.test(prefix)) {
            throwError(state, 'ill-formed tag prefix (second argument) of the TAG directive');
        }

        state.tagMap[handle] = prefix;
    }
};


function captureSegment(state, start, end, checkJson) {
    var _position, _length, _character, _result;

    if (start < end) {
        _result = state.input.slice(start, end);

        if (checkJson) {
            for (_position = 0, _length = _result.length;
                _position < _length;
                _position += 1) {
                _character = _result.charCodeAt(_position);
                if (!(_character === 0x09 ||
                    (0x20 <= _character && _character <= 0x10FFFF))) {
                    throwError(state, 'expected valid JSON character');
                }
            }
        } else if (PATTERN_NON_PRINTABLE.test(_result)) {
            throwError(state, 'the stream contains non-printable characters');
        }

        state.result += _result;
    }
}

function mergeMappings(state, destination, source, overridableKeys) {
    var sourceKeys, key, index, quantity;

    if (!common.isObject(source)) {
        throwError(state, 'cannot merge mappings; the provided source object is unacceptable');
    }

    sourceKeys = Object.keys(source);

    for (index = 0, quantity = sourceKeys.length; index < quantity; index += 1) {
        key = sourceKeys[index];

        if (!_hasOwnProperty.call(destination, key)) {
            destination[key] = source[key];
            overridableKeys[key] = true;
        }
    }
}

function storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, valueNode) {
    var index, quantity;

    keyNode = String(keyNode);

    if (_result === null) {
        _result = {};
    }

    if (keyTag === 'tag:yaml.org,2002:merge') {
        if (Array.isArray(valueNode)) {
            for (index = 0, quantity = valueNode.length; index < quantity; index += 1) {
                mergeMappings(state, _result, valueNode[index], overridableKeys);
            }
        } else {
            mergeMappings(state, _result, valueNode, overridableKeys);
        }
    } else {
        if (!state.json &&
            !_hasOwnProperty.call(overridableKeys, keyNode) &&
            _hasOwnProperty.call(_result, keyNode)) {
            throwError(state, 'duplicated mapping key');
        }
        _result[keyNode] = valueNode;
        delete overridableKeys[keyNode];
    }

    return _result;
}

function readLineBreak(state) {
    var ch;

    ch = state.input.charCodeAt(state.position);

    if (ch === 0x0A/* LF */) {
        state.position++;
    } else if (ch === 0x0D/* CR */) {
        state.position++;
        if (state.input.charCodeAt(state.position) === 0x0A/* LF */) {
            state.position++;
        }
    } else {
        throwError(state, 'a line break is expected');
    }

    state.line += 1;
    state.lineStart = state.position;
}

function skipSeparationSpace(state, allowComments, checkIndent) {
    var lineBreaks = 0,
        ch = state.input.charCodeAt(state.position);

    while (ch !== 0) {
        while (is_WHITE_SPACE(ch)) {
            ch = state.input.charCodeAt(++state.position);
        }

        if (allowComments && ch === 0x23/* # */) {
            do {
                ch = state.input.charCodeAt(++state.position);
            } while (ch !== 0x0A/* LF */ && ch !== 0x0D/* CR */ && ch !== 0);
        }

        if (is_EOL(ch)) {
            readLineBreak(state);

            ch = state.input.charCodeAt(state.position);
            lineBreaks++;
            state.lineIndent = 0;

            while (ch === 0x20/* Space */) {
                state.lineIndent++;
                ch = state.input.charCodeAt(++state.position);
            }
        } else {
            break;
        }
    }

    if (checkIndent !== -1 && lineBreaks !== 0 && state.lineIndent < checkIndent) {
        throwWarning(state, 'deficient indentation');
    }

    return lineBreaks;
}

function testDocumentSeparator(state) {
    var _position = state.position,
        ch;

    ch = state.input.charCodeAt(_position);

    // Condition state.position === state.lineStart is tested
    // in parent on each call, for efficiency. No needs to test here again.
    if ((ch === 0x2D/* - */ || ch === 0x2E/* . */) &&
        ch === state.input.charCodeAt(_position + 1) &&
        ch === state.input.charCodeAt(_position + 2)) {

        _position += 3;

        ch = state.input.charCodeAt(_position);

        if (ch === 0 || is_WS_OR_EOL(ch)) {
            return true;
        }
    }

    return false;
}

function writeFoldedLines(state, count) {
    if (count === 1) {
        state.result += ' ';
    } else if (count > 1) {
        state.result += common.repeat('\n', count - 1);
    }
}


function readPlainScalar(state, nodeIndent, withinFlowCollection) {
    var preceding,
        following,
        captureStart,
        captureEnd,
        hasPendingContent,
        _line,
        _lineStart,
        _lineIndent,
        _kind = state.kind,
        _result = state.result,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (is_WS_OR_EOL(ch) ||
        is_FLOW_INDICATOR(ch) ||
        ch === 0x23/* # */ ||
        ch === 0x26/* & */ ||
        ch === 0x2A/* * */ ||
        ch === 0x21/* ! */ ||
        ch === 0x7C/* | */ ||
        ch === 0x3E/* > */ ||
        ch === 0x27/* ' */ ||
        ch === 0x22/* " */ ||
        ch === 0x25/* % */ ||
        ch === 0x40/* @ */ ||
        ch === 0x60/* ` */) {
        return false;
    }

    if (ch === 0x3F/* ? */ || ch === 0x2D/* - */) {
        following = state.input.charCodeAt(state.position + 1);

        if (is_WS_OR_EOL(following) ||
            withinFlowCollection && is_FLOW_INDICATOR(following)) {
            return false;
        }
    }

    state.kind = 'scalar';
    state.result = '';
    captureStart = captureEnd = state.position;
    hasPendingContent = false;

    while (ch !== 0) {
        if (ch === 0x3A/* : */) {
            following = state.input.charCodeAt(state.position + 1);

            if (is_WS_OR_EOL(following) ||
                withinFlowCollection && is_FLOW_INDICATOR(following)) {
                break;
            }

        } else if (ch === 0x23/* # */) {
            preceding = state.input.charCodeAt(state.position - 1);

            if (is_WS_OR_EOL(preceding)) {
                break;
            }

        } else if ((state.position === state.lineStart && testDocumentSeparator(state)) ||
            withinFlowCollection && is_FLOW_INDICATOR(ch)) {
            break;

        } else if (is_EOL(ch)) {
            _line = state.line;
            _lineStart = state.lineStart;
            _lineIndent = state.lineIndent;
            skipSeparationSpace(state, false, -1);

            if (state.lineIndent >= nodeIndent) {
                hasPendingContent = true;
                ch = state.input.charCodeAt(state.position);
                continue;
            } else {
                state.position = captureEnd;
                state.line = _line;
                state.lineStart = _lineStart;
                state.lineIndent = _lineIndent;
                break;
            }
        }

        if (hasPendingContent) {
            captureSegment(state, captureStart, captureEnd, false);
            writeFoldedLines(state, state.line - _line);
            captureStart = captureEnd = state.position;
            hasPendingContent = false;
        }

        if (!is_WHITE_SPACE(ch)) {
            captureEnd = state.position + 1;
        }

        ch = state.input.charCodeAt(++state.position);
    }

    captureSegment(state, captureStart, captureEnd, false);

    if (state.result) {
        return true;
    }

    state.kind = _kind;
    state.result = _result;
    return false;
}

function readSingleQuotedScalar(state, nodeIndent) {
    var ch,
        captureStart, captureEnd;

    ch = state.input.charCodeAt(state.position);

    if (ch !== 0x27/* ' */) {
        return false;
    }

    state.kind = 'scalar';
    state.result = '';
    state.position++;
    captureStart = captureEnd = state.position;

    while ((ch = state.input.charCodeAt(state.position)) !== 0) {
        if (ch === 0x27/* ' */) {
            captureSegment(state, captureStart, state.position, true);
            ch = state.input.charCodeAt(++state.position);

            if (ch === 0x27/* ' */) {
                captureStart = state.position;
                state.position++;
                captureEnd = state.position;
            } else {
                return true;
            }

        } else if (is_EOL(ch)) {
            captureSegment(state, captureStart, captureEnd, true);
            writeFoldedLines(state, skipSeparationSpace(state, false, nodeIndent));
            captureStart = captureEnd = state.position;

        } else if (state.position === state.lineStart && testDocumentSeparator(state)) {
            throwError(state, 'unexpected end of the document within a single quoted scalar');

        } else {
            state.position++;
            captureEnd = state.position;
        }
    }

    throwError(state, 'unexpected end of the stream within a single quoted scalar');
}

function readDoubleQuotedScalar(state, nodeIndent) {
    var captureStart,
        captureEnd,
        hexLength,
        hexResult,
        tmp,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch !== 0x22/* " */) {
        return false;
    }

    state.kind = 'scalar';
    state.result = '';
    state.position++;
    captureStart = captureEnd = state.position;

    while ((ch = state.input.charCodeAt(state.position)) !== 0) {
        if (ch === 0x22/* " */) {
            captureSegment(state, captureStart, state.position, true);
            state.position++;
            return true;

        } else if (ch === 0x5C/* \ */) {
            captureSegment(state, captureStart, state.position, true);
            ch = state.input.charCodeAt(++state.position);

            if (is_EOL(ch)) {
                skipSeparationSpace(state, false, nodeIndent);

                // TODO: rework to inline fn with no type cast?
            } else if (ch < 256 && simpleEscapeCheck[ch]) {
                state.result += simpleEscapeMap[ch];
                state.position++;

            } else if ((tmp = escapedHexLen(ch)) > 0) {
                hexLength = tmp;
                hexResult = 0;

                for (; hexLength > 0; hexLength--) {
                    ch = state.input.charCodeAt(++state.position);

                    if ((tmp = fromHexCode(ch)) >= 0) {
                        hexResult = (hexResult << 4) + tmp;

                    } else {
                        throwError(state, 'expected hexadecimal character');
                    }
                }

                state.result += charFromCodepoint(hexResult);

                state.position++;

            } else {
                throwError(state, 'unknown escape sequence');
            }

            captureStart = captureEnd = state.position;

        } else if (is_EOL(ch)) {
            captureSegment(state, captureStart, captureEnd, true);
            writeFoldedLines(state, skipSeparationSpace(state, false, nodeIndent));
            captureStart = captureEnd = state.position;

        } else if (state.position === state.lineStart && testDocumentSeparator(state)) {
            throwError(state, 'unexpected end of the document within a double quoted scalar');

        } else {
            state.position++;
            captureEnd = state.position;
        }
    }

    throwError(state, 'unexpected end of the stream within a double quoted scalar');
}

function readFlowCollection(state, nodeIndent) {
    var readNext = true,
        _line,
        _tag = state.tag,
        _result,
        _anchor = state.anchor,
        following,
        terminator,
        isPair,
        isExplicitPair,
        isMapping,
        overridableKeys = {},
        keyNode,
        keyTag,
        valueNode,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch === 0x5B/* [ */) {
        terminator = 0x5D;/* ] */
        isMapping = false;
        _result = [];
    } else if (ch === 0x7B/* { */) {
        terminator = 0x7D;/* } */
        isMapping = true;
        _result = {};
    } else {
        return false;
    }

    if (state.anchor !== null) {
        state.anchorMap[state.anchor] = _result;
    }

    ch = state.input.charCodeAt(++state.position);

    while (ch !== 0) {
        skipSeparationSpace(state, true, nodeIndent);

        ch = state.input.charCodeAt(state.position);

        if (ch === terminator) {
            state.position++;
            state.tag = _tag;
            state.anchor = _anchor;
            state.kind = isMapping ? 'mapping' : 'sequence';
            state.result = _result;
            return true;
        } else if (!readNext) {
            throwError(state, 'missed comma between flow collection entries');
        }

        keyTag = keyNode = valueNode = null;
        isPair = isExplicitPair = false;

        if (ch === 0x3F/* ? */) {
            following = state.input.charCodeAt(state.position + 1);

            if (is_WS_OR_EOL(following)) {
                isPair = isExplicitPair = true;
                state.position++;
                skipSeparationSpace(state, true, nodeIndent);
            }
        }

        _line = state.line;
        composeNode(state, nodeIndent, CONTEXT_FLOW_IN, false, true);
        keyTag = state.tag;
        keyNode = state.result;
        skipSeparationSpace(state, true, nodeIndent);

        ch = state.input.charCodeAt(state.position);

        if ((isExplicitPair || state.line === _line) && ch === 0x3A/* : */) {
            isPair = true;
            ch = state.input.charCodeAt(++state.position);
            skipSeparationSpace(state, true, nodeIndent);
            composeNode(state, nodeIndent, CONTEXT_FLOW_IN, false, true);
            valueNode = state.result;
        }

        if (isMapping) {
            storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, valueNode);
        } else if (isPair) {
            _result.push(storeMappingPair(state, null, overridableKeys, keyTag, keyNode, valueNode));
        } else {
            _result.push(keyNode);
        }

        skipSeparationSpace(state, true, nodeIndent);

        ch = state.input.charCodeAt(state.position);

        if (ch === 0x2C/* , */) {
            readNext = true;
            ch = state.input.charCodeAt(++state.position);
        } else {
            readNext = false;
        }
    }

    throwError(state, 'unexpected end of the stream within a flow collection');
}

function readBlockScalar(state, nodeIndent) {
    var captureStart,
        folding,
        chomping = CHOMPING_CLIP,
        didReadContent = false,
        detectedIndent = false,
        textIndent = nodeIndent,
        emptyLines = 0,
        atMoreIndented = false,
        tmp,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch === 0x7C/* | */) {
        folding = false;
    } else if (ch === 0x3E/* > */) {
        folding = true;
    } else {
        return false;
    }

    state.kind = 'scalar';
    state.result = '';

    while (ch !== 0) {
        ch = state.input.charCodeAt(++state.position);

        if (ch === 0x2B/* + */ || ch === 0x2D/* - */) {
            if (CHOMPING_CLIP === chomping) {
                chomping = (ch === 0x2B/* + */) ? CHOMPING_KEEP : CHOMPING_STRIP;
            } else {
                throwError(state, 'repeat of a chomping mode identifier');
            }

        } else if ((tmp = fromDecimalCode(ch)) >= 0) {
            if (tmp === 0) {
                throwError(state, 'bad explicit indentation width of a block scalar; it cannot be less than one');
            } else if (!detectedIndent) {
                textIndent = nodeIndent + tmp - 1;
                detectedIndent = true;
            } else {
                throwError(state, 'repeat of an indentation width identifier');
            }

        } else {
            break;
        }
    }

    if (is_WHITE_SPACE(ch)) {
        do { ch = state.input.charCodeAt(++state.position); }
        while (is_WHITE_SPACE(ch));

        if (ch === 0x23/* # */) {
            do { ch = state.input.charCodeAt(++state.position); }
            while (!is_EOL(ch) && (ch !== 0));
        }
    }

    while (ch !== 0) {
        readLineBreak(state);
        state.lineIndent = 0;

        ch = state.input.charCodeAt(state.position);

        while ((!detectedIndent || state.lineIndent < textIndent) &&
            (ch === 0x20/* Space */)) {
            state.lineIndent++;
            ch = state.input.charCodeAt(++state.position);
        }

        if (!detectedIndent && state.lineIndent > textIndent) {
            textIndent = state.lineIndent;
        }

        if (is_EOL(ch)) {
            emptyLines++;
            continue;
        }

        // End of the scalar.
        if (state.lineIndent < textIndent) {

            // Perform the chomping.
            if (chomping === CHOMPING_KEEP) {
                state.result += common.repeat('\n', didReadContent ? 1 + emptyLines : emptyLines);
            } else if (chomping === CHOMPING_CLIP) {
                if (didReadContent) { // i.e. only if the scalar is not empty.
                    state.result += '\n';
                }
            }

            // Break this `while` cycle and go to the funciton's epilogue.
            break;
        }

        // Folded style: use fancy rules to handle line breaks.
        if (folding) {

            // Lines starting with white space characters (more-indented lines) are not folded.
            if (is_WHITE_SPACE(ch)) {
                atMoreIndented = true;
                // except for the first content line (cf. Example 8.1)
                state.result += common.repeat('\n', didReadContent ? 1 + emptyLines : emptyLines);

                // End of more-indented block.
            } else if (atMoreIndented) {
                atMoreIndented = false;
                state.result += common.repeat('\n', emptyLines + 1);

                // Just one line break - perceive as the same line.
            } else if (emptyLines === 0) {
                if (didReadContent) { // i.e. only if we have already read some scalar content.
                    state.result += ' ';
                }

                // Several line breaks - perceive as different lines.
            } else {
                state.result += common.repeat('\n', emptyLines);
            }

            // Literal style: just add exact number of line breaks between content lines.
        } else {
            // Keep all line breaks except the header line break.
            state.result += common.repeat('\n', didReadContent ? 1 + emptyLines : emptyLines);
        }

        didReadContent = true;
        detectedIndent = true;
        emptyLines = 0;
        captureStart = state.position;

        while (!is_EOL(ch) && (ch !== 0)) {
            ch = state.input.charCodeAt(++state.position);
        }

        captureSegment(state, captureStart, state.position, false);
    }

    return true;
}

function readBlockSequence(state, nodeIndent) {
    var _line,
        _tag = state.tag,
        _anchor = state.anchor,
        _result = [],
        following,
        detected = false,
        ch;

    if (state.anchor !== null) {
        state.anchorMap[state.anchor] = _result;
    }

    ch = state.input.charCodeAt(state.position);

    while (ch !== 0) {

        if (ch !== 0x2D/* - */) {
            break;
        }

        following = state.input.charCodeAt(state.position + 1);

        if (!is_WS_OR_EOL(following)) {
            break;
        }

        detected = true;
        state.position++;

        if (skipSeparationSpace(state, true, -1)) {
            if (state.lineIndent <= nodeIndent) {
                _result.push(null);
                ch = state.input.charCodeAt(state.position);
                continue;
            }
        }

        _line = state.line;
        composeNode(state, nodeIndent, CONTEXT_BLOCK_IN, false, true);
        _result.push(state.result);
        skipSeparationSpace(state, true, -1);

        ch = state.input.charCodeAt(state.position);

        if ((state.line === _line || state.lineIndent > nodeIndent) && (ch !== 0)) {
            throwError(state, 'bad indentation of a sequence entry');
        } else if (state.lineIndent < nodeIndent) {
            break;
        }
    }

    if (detected) {
        state.tag = _tag;
        state.anchor = _anchor;
        state.kind = 'sequence';
        state.result = _result;
        return true;
    }
    return false;
}

function readBlockMapping(state, nodeIndent, flowIndent) {
    var following,
        allowCompact,
        _line,
        _tag = state.tag,
        _anchor = state.anchor,
        _result = {},
        overridableKeys = {},
        keyTag = null,
        keyNode = null,
        valueNode = null,
        atExplicitKey = false,
        detected = false,
        ch;

    var startLine = state.line;
    var startColumn = state.position - state.lineStart;
    var startIndex = state.position;
    //console.log("START...");
    //var cloned = JSON.parse(JSON.stringify(state));
    //delete cloned["typeMap"];
    //delete cloned["schema"];
    //delete cloned["implicitTypes"];
    //console.log(JSON.stringify(cloned, null, 2));
    if (state.anchor !== null) {
        state.anchorMap[state.anchor] = _result;
    }

    ch = state.input.charCodeAt(state.position);

    while (ch !== 0) {
        following = state.input.charCodeAt(state.position + 1);
        _line = state.line; // Save the current line.

        //
        // Explicit notation case. There are two separate blocks:
        // first for the key (denoted by "?") and second for the value (denoted by ":")
        //
        if ((ch === 0x3F/* ? */ || ch === 0x3A/* : */) && is_WS_OR_EOL(following)) {

            if (ch === 0x3F/* ? */) {
                if (atExplicitKey) {
                    storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, null);
                    keyTag = keyNode = valueNode = null;
                }

                detected = true;
                atExplicitKey = true;
                allowCompact = true;

            } else if (atExplicitKey) {
                // i.e. 0x3A/* : */ === character after the explicit key.
                atExplicitKey = false;
                allowCompact = true;

            } else {
                throwError(state, 'incomplete explicit mapping pair; a key node is missed');
            }

            state.position += 1;
            ch = following;

            //
            // Implicit notation case. Flow-style node as the key first, then ":", and the value.
            //
        } else if (composeNode(state, flowIndent, CONTEXT_FLOW_OUT, false, true)) {

            var endLine = state.line;
            var endColumn = state.position - state.lineStart;
            var endIndex = state.position;
            //console.log("END...");
            //var cloned = JSON.parse(JSON.stringify(state));
            //delete cloned["typeMap"];
            //delete cloned["schema"];
            //delete cloned["implicitTypes"];
            //console.log(JSON.stringify(cloned, null, 2));

            if (state.line === _line) {
                ch = state.input.charCodeAt(state.position);

                while (is_WHITE_SPACE(ch)) {
                    ch = state.input.charCodeAt(++state.position);
                }

                if (ch === 0x3A/* : */) {
                    ch = state.input.charCodeAt(++state.position);

                    if (!is_WS_OR_EOL(ch)) {
                        throwError(state, 'a whitespace character is expected after the key-value separator within a block mapping');
                    }

                    if (atExplicitKey) {
                        storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, null);
                        keyTag = keyNode = valueNode = null;
                    }

                    detected = true;
                    atExplicitKey = false;
                    allowCompact = false;
                    keyTag = state.tag;
                    keyNode = state.result;

                } else if (detected) {
                    throwError(state, 'can not read an implicit mapping pair; a colon is missed');

                } else {
                    state.tag = _tag;
                    state.anchor = _anchor;
                    return true; // Keep the result of `composeNode`.
                }

            } else if (detected) {
                throwError(state, 'can not read a block mapping entry; a multiline key may not be an implicit key');

            } else {
                state.tag = _tag;
                state.anchor = _anchor;

                return true; // Keep the result of `composeNode`.
            }

        } else {
            break; // Reading is done. Go to the epilogue.
        }

        //
        // Common reading code for both explicit and implicit notations.
        //
        if (state.line === _line || state.lineIndent > nodeIndent) {
            if (composeNode(state, nodeIndent, CONTEXT_BLOCK_OUT, true, allowCompact)) {
                if (atExplicitKey) {
                    keyNode = state.result;
                } else {
                    valueNode = state.result;
                }
            }

            if (!atExplicitKey) {
                storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, valueNode);
                keyTag = keyNode = valueNode = null;
            }

            skipSeparationSpace(state, true, -1);
            ch = state.input.charCodeAt(state.position);
        }

        if (state.lineIndent > nodeIndent && (ch !== 0)) {
            throwError(state, 'bad indentation of a mapping entry');
        } else if (state.lineIndent < nodeIndent) {
            break;
        }
    }

    //
    // Epilogue.
    //

    // Special case: last mapping's node contains only the key in explicit notation.
    if (atExplicitKey) {
        storeMappingPair(state, _result, overridableKeys, keyTag, keyNode, null);
    }

    // Expose the resulting mapping.
    if (detected) {

        var endLine = state.line;
        var endColumn = state.position - state.lineStart;
        var endIndex = state.position;

        var positions = {
            "start-line": startLine,
            "start-column": startColumn,
            "start-index": startIndex,
            "end-line": endLine,
            "end-column": endColumn,
            "end-index": endIndex
        };
        _result["__location__"] = positions;
        state.tag = _tag;
        state.anchor = _anchor;
        state.kind = 'mapping';
        state.result = _result;
    }

    return detected;
}

function readTagProperty(state) {
    var _position,
        isVerbatim = false,
        isNamed = false,
        tagHandle,
        tagName,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch !== 0x21/* ! */) return false;

    if (state.tag !== null) {
        throwError(state, 'duplication of a tag property');
    }

    ch = state.input.charCodeAt(++state.position);

    if (ch === 0x3C/* < */) {
        isVerbatim = true;
        ch = state.input.charCodeAt(++state.position);

    } else if (ch === 0x21/* ! */) {
        isNamed = true;
        tagHandle = '!!';
        ch = state.input.charCodeAt(++state.position);

    } else {
        tagHandle = '!';
    }

    _position = state.position;

    if (isVerbatim) {
        do { ch = state.input.charCodeAt(++state.position); }
        while (ch !== 0 && ch !== 0x3E/* > */);

        if (state.position < state.length) {
            tagName = state.input.slice(_position, state.position);
            ch = state.input.charCodeAt(++state.position);
        } else {
            throwError(state, 'unexpected end of the stream within a verbatim tag');
        }
    } else {
        while (ch !== 0 && !is_WS_OR_EOL(ch)) {

            if (ch === 0x21/* ! */) {
                if (!isNamed) {
                    tagHandle = state.input.slice(_position - 1, state.position + 1);

                    if (!PATTERN_TAG_HANDLE.test(tagHandle)) {
                        throwError(state, 'named tag handle cannot contain such characters');
                    }

                    isNamed = true;
                    _position = state.position + 1;
                } else {
                    throwError(state, 'tag suffix cannot contain exclamation marks');
                }
            }

            ch = state.input.charCodeAt(++state.position);
        }

        tagName = state.input.slice(_position, state.position);

        if (PATTERN_FLOW_INDICATORS.test(tagName)) {
            throwError(state, 'tag suffix cannot contain flow indicator characters');
        }
    }

    if (tagName && !PATTERN_TAG_URI.test(tagName)) {
        throwError(state, 'tag name cannot contain such characters: ' + tagName);
    }

    if (isVerbatim) {
        state.tag = tagName;

    } else if (_hasOwnProperty.call(state.tagMap, tagHandle)) {
        state.tag = state.tagMap[tagHandle] + tagName;

    } else if (tagHandle === '!') {
        state.tag = '!' + tagName;

    } else if (tagHandle === '!!') {
        state.tag = 'tag:yaml.org,2002:' + tagName;

    } else {
        throwError(state, 'undeclared tag handle "' + tagHandle + '"');
    }

    return true;
}

function readAnchorProperty(state) {
    var _position,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch !== 0x26/* & */) return false;

    if (state.anchor !== null) {
        throwError(state, 'duplication of an anchor property');
    }

    ch = state.input.charCodeAt(++state.position);
    _position = state.position;

    while (ch !== 0 && !is_WS_OR_EOL(ch) && !is_FLOW_INDICATOR(ch)) {
        ch = state.input.charCodeAt(++state.position);
    }

    if (state.position === _position) {
        throwError(state, 'name of an anchor node must contain at least one character');
    }

    state.anchor = state.input.slice(_position, state.position);
    return true;
}

function readAlias(state) {
    var _position, alias,
        ch;

    ch = state.input.charCodeAt(state.position);

    if (ch !== 0x2A/* * */) return false;

    ch = state.input.charCodeAt(++state.position);
    _position = state.position;

    while (ch !== 0 && !is_WS_OR_EOL(ch) && !is_FLOW_INDICATOR(ch)) {
        ch = state.input.charCodeAt(++state.position);
    }

    if (state.position === _position) {
        throwError(state, 'name of an alias node must contain at least one character');
    }

    alias = state.input.slice(_position, state.position);

    if (!state.anchorMap.hasOwnProperty(alias)) {
        throwError(state, 'unidentified alias "' + alias + '"');
    }

    state.result = state.anchorMap[alias];
    skipSeparationSpace(state, true, -1);
    return true;
}

function composeNode(state, parentIndent, nodeContext, allowToSeek, allowCompact) {
    var allowBlockStyles,
        allowBlockScalars,
        allowBlockCollections,
        indentStatus = 1, // 1: this>parent, 0: this=parent, -1: this<parent
        atNewLine = false,
        hasContent = false,
        typeIndex,
        typeQuantity,
        type,
        flowIndent,
        blockIndent;

    if (state.listener !== null) {
        state.listener('open', state);
    }

    state.tag = null;
    state.anchor = null;
    state.kind = null;
    state.result = null;

    var startLine = state.line;
    var startColumn = state.lineStart;
    var startIndex = state.position;

    allowBlockStyles = allowBlockScalars = allowBlockCollections =
        CONTEXT_BLOCK_OUT === nodeContext ||
        CONTEXT_BLOCK_IN === nodeContext;

    if (allowToSeek) {
        if (skipSeparationSpace(state, true, -1)) {
            atNewLine = true;

            if (state.lineIndent > parentIndent) {
                indentStatus = 1;
            } else if (state.lineIndent === parentIndent) {
                indentStatus = 0;
            } else if (state.lineIndent < parentIndent) {
                indentStatus = -1;
            }
        }
    }

    if (indentStatus === 1) {
        while (readTagProperty(state) || readAnchorProperty(state)) {
            if (skipSeparationSpace(state, true, -1)) {
                atNewLine = true;
                allowBlockCollections = allowBlockStyles;

                if (state.lineIndent > parentIndent) {
                    indentStatus = 1;
                } else if (state.lineIndent === parentIndent) {
                    indentStatus = 0;
                } else if (state.lineIndent < parentIndent) {
                    indentStatus = -1;
                }
            } else {
                allowBlockCollections = false;
            }
        }
    }

    if (allowBlockCollections) {
        allowBlockCollections = atNewLine || allowCompact;
    }

    if (indentStatus === 1 || CONTEXT_BLOCK_OUT === nodeContext) {
        if (CONTEXT_FLOW_IN === nodeContext || CONTEXT_FLOW_OUT === nodeContext) {
            flowIndent = parentIndent;
        } else {
            flowIndent = parentIndent + 1;
        }

        blockIndent = state.position - state.lineStart;

        if (indentStatus === 1) {
            if (allowBlockCollections &&
                (readBlockSequence(state, blockIndent) ||
                    readBlockMapping(state, blockIndent, flowIndent)) ||
                readFlowCollection(state, flowIndent)) {
                hasContent = true;
            } else {
                if ((allowBlockScalars && readBlockScalar(state, flowIndent)) ||
                    readSingleQuotedScalar(state, flowIndent) ||
                    readDoubleQuotedScalar(state, flowIndent)) {
                    hasContent = true;

                } else if (readAlias(state)) {
                    hasContent = true;

                    if (state.tag !== null || state.anchor !== null) {
                        throwError(state, 'alias node should not have any properties');
                    }

                } else if (readPlainScalar(state, flowIndent, CONTEXT_FLOW_IN === nodeContext)) {
                    hasContent = true;

                    if (state.tag === null) {
                        state.tag = '?';
                    }
                }

                if (state.anchor !== null) {
                    state.anchorMap[state.anchor] = state.result;
                }
            }
        } else if (indentStatus === 0) {
            // Special case: block sequences are allowed to have same indentation level as the parent.
            // http://www.yaml.org/spec/1.2/spec.html#id2799784
            hasContent = allowBlockCollections && readBlockSequence(state, blockIndent);
        }
    }

    if (state.tag !== null && state.tag !== '!') {
        if (state.tag === '?') {
            for (typeIndex = 0, typeQuantity = state.implicitTypes.length;
                typeIndex < typeQuantity;
                typeIndex += 1) {
                type = state.implicitTypes[typeIndex];

                // Implicit resolving is not allowed for non-scalar types, and '?'
                // non-specific tag is only assigned to plain scalars. So, it isn't
                // needed to check for 'kind' conformity.

                if (type.resolve(state.result)) { // `state.result` updated in resolver if matched
                    state.result = type.construct(state.result);
                    state.tag = type.tag;
                    if (state.anchor !== null) {
                        state.anchorMap[state.anchor] = state.result;
                    }
                    break;
                }
            }
        } else if (_hasOwnProperty.call(state.typeMap[state.kind || 'fallback'], state.tag)) {
            type = state.typeMap[state.kind || 'fallback'][state.tag];

            if (state.result !== null && type.kind !== state.kind) {
                throwError(state, 'unacceptable node kind for !<' + state.tag + '> tag; it should be "' + type.kind + '", not "' + state.kind + '"');
            }

            if (!type.resolve(state.result)) { // `state.result` updated in resolver if matched
                throwError(state, 'cannot resolve a node with !<' + state.tag + '> explicit tag');
            } else {
                state.result = type.construct(state.result);
                if (state.anchor !== null) {
                    state.anchorMap[state.anchor] = state.result;
                }
            }
        } else {
            debugger;
            throwError(state, 'unknown tag !<' + state.tag + '>');
        }
    }

    if (state.listener !== null) {
        state.listener('close', state);
    }


    var endLine = state.line;
    var endColumn = state.position - state.lineStart;
    var endIndex = state.position;

    // console.log("RETURNINNG");
    // var cloned = JSON.parse(JSON.stringify(state));
    // delete cloned["schema"];
    // delete cloned["implicitTypes"];
    // delete cloned["typeMap"];
    // console.log(JSON.stringify(cloned, null, 2));
    return state.tag !== null || state.anchor !== null || hasContent;
}

function readDocument(state) {
    var documentStart = state.position,
        _position,
        directiveName,
        directiveArgs,
        hasDirectives = false,
        ch;

    state.version = null;
    state.checkLineBreaks = state.legacy;
    state.tagMap = {};
    state.anchorMap = {};

    while ((ch = state.input.charCodeAt(state.position)) !== 0) {
        skipSeparationSpace(state, true, -1);

        ch = state.input.charCodeAt(state.position);

        if (state.lineIndent > 0 || ch !== 0x25/* % */) {
            break;
        }

        hasDirectives = true;
        ch = state.input.charCodeAt(++state.position);
        _position = state.position;

        while (ch !== 0 && !is_WS_OR_EOL(ch)) {
            ch = state.input.charCodeAt(++state.position);
        }

        directiveName = state.input.slice(_position, state.position);
        directiveArgs = [];

        if (directiveName.length < 1) {
            throwError(state, 'directive name must not be less than one character in length');
        }

        while (ch !== 0) {
            while (is_WHITE_SPACE(ch)) {
                ch = state.input.charCodeAt(++state.position);
            }

            if (ch === 0x23/* # */) {
                do { ch = state.input.charCodeAt(++state.position); }
                while (ch !== 0 && !is_EOL(ch));
                break;
            }

            if (is_EOL(ch)) break;

            _position = state.position;

            while (ch !== 0 && !is_WS_OR_EOL(ch)) {
                ch = state.input.charCodeAt(++state.position);
            }

            directiveArgs.push(state.input.slice(_position, state.position));
        }

        if (ch !== 0) readLineBreak(state);

        if (_hasOwnProperty.call(directiveHandlers, directiveName)) {
            directiveHandlers[directiveName](state, directiveName, directiveArgs);
        } else {
            throwWarning(state, 'unknown document directive "' + directiveName + '"');
        }
    }

    skipSeparationSpace(state, true, -1);

    if (state.lineIndent === 0 &&
        state.input.charCodeAt(state.position) === 0x2D/* - */ &&
        state.input.charCodeAt(state.position + 1) === 0x2D/* - */ &&
        state.input.charCodeAt(state.position + 2) === 0x2D/* - */) {
        state.position += 3;
        skipSeparationSpace(state, true, -1);

    } else if (hasDirectives) {
        throwError(state, 'directives end mark is expected');
    }

    composeNode(state, state.lineIndent - 1, CONTEXT_BLOCK_OUT, false, true);
    skipSeparationSpace(state, true, -1);

    if (state.checkLineBreaks &&
        PATTERN_NON_ASCII_LINE_BREAKS.test(state.input.slice(documentStart, state.position))) {
        throwWarning(state, 'non-ASCII line breaks are interpreted as content');
    }

    state.documents.push(state.result);

    if (state.position === state.lineStart && testDocumentSeparator(state)) {

        if (state.input.charCodeAt(state.position) === 0x2E/* . */) {
            state.position += 3;
            skipSeparationSpace(state, true, -1);
        }
        return;
    }

    if (state.position < (state.length - 1)) {
        throwError(state, 'end of the stream or a document separator is expected');
    } else {
        return;
    }
}


function loadDocuments(input, options) {
    input = String(input);
    options = options || {};

    if (input.length !== 0) {

        // Add tailing `\n` if not exists
        if (input.charCodeAt(input.length - 1) !== 0x0A/* LF */ &&
            input.charCodeAt(input.length - 1) !== 0x0D/* CR */) {
            input += '\n';
        }

        // Strip BOM
        if (input.charCodeAt(0) === 0xFEFF) {
            input = input.slice(1);
        }
    }

    var state = new State(input, options);

    // Use 0 as string terminator. That significantly simplifies bounds check.
    state.input += '\0';

    while (state.input.charCodeAt(state.position) === 0x20/* Space */) {
        state.lineIndent += 1;
        state.position += 1;
    }

    while (state.position < (state.length - 1)) {
        readDocument(state);
    }

    return state.documents;
}


function loadAll(input, iterator, options) {
    var documents = loadDocuments(input, options), index, length;

    for (index = 0, length = documents.length; index < length; index += 1) {
        iterator(documents[index]);
    }
}


function load(input, options) {
    var documents = loadDocuments(input, options);

    if (documents.length === 0) {
        /*eslint-disable no-undefined*/
        return undefined;
    } else if (documents.length === 1) {
        return documents[0];
    }
    throw new YAMLException('expected a single document in the stream, but found more');
}


function safeLoadAll(input, output, options) {
    loadAll(input, output, common.extend({ schema: DEFAULT_SAFE_SCHEMA }, options));
}


function safeLoad(input, options) {
    return load(input, common.extend({ schema: DEFAULT_SAFE_SCHEMA }, options));
}


module.exports.loadAll = loadAll;
module.exports.load = load;
module.exports.safeLoadAll = safeLoadAll;
module.exports.safeLoad = safeLoad;

},{"./common":3,"./exception":5,"./mark":7,"./schema/default_full":10,"./schema/default_safe":11}],7:[function(require,module,exports){
'use strict';


var common = require('./common');


function Mark(name, buffer, position, line, column) {
  this.name     = name;
  this.buffer   = buffer;
  this.position = position;
  this.line     = line;
  this.column   = column;
}


Mark.prototype.getSnippet = function getSnippet(indent, maxLength) {
  var head, start, tail, end, snippet;

  if (!this.buffer) return null;

  indent = indent || 4;
  maxLength = maxLength || 75;

  head = '';
  start = this.position;

  while (start > 0 && '\x00\r\n\x85\u2028\u2029'.indexOf(this.buffer.charAt(start - 1)) === -1) {
    start -= 1;
    if (this.position - start > (maxLength / 2 - 1)) {
      head = ' ... ';
      start += 5;
      break;
    }
  }

  tail = '';
  end = this.position;

  while (end < this.buffer.length && '\x00\r\n\x85\u2028\u2029'.indexOf(this.buffer.charAt(end)) === -1) {
    end += 1;
    if (end - this.position > (maxLength / 2 - 1)) {
      tail = ' ... ';
      end -= 5;
      break;
    }
  }

  snippet = this.buffer.slice(start, end);

  return common.repeat(' ', indent) + head + snippet + tail + '\n' +
         common.repeat(' ', indent + this.position - start + head.length) + '^';
};


Mark.prototype.toString = function toString(compact) {
  var snippet, where = '';

  if (this.name) {
    where += 'in "' + this.name + '" ';
  }

  where += 'at line ' + (this.line + 1) + ', column ' + (this.column + 1);

  if (!compact) {
    snippet = this.getSnippet();

    if (snippet) {
      where += ':\n' + snippet;
    }
  }

  return where;
};


module.exports = Mark;

},{"./common":3}],8:[function(require,module,exports){
'use strict';

/*eslint-disable max-len*/

var common        = require('./common');
var YAMLException = require('./exception');
var Type          = require('./type');


function compileList(schema, name, result) {
  var exclude = [];

  schema.include.forEach(function (includedSchema) {
    result = compileList(includedSchema, name, result);
  });

  schema[name].forEach(function (currentType) {
    result.forEach(function (previousType, previousIndex) {
      if (previousType.tag === currentType.tag && previousType.kind === currentType.kind) {
        exclude.push(previousIndex);
      }
    });

    result.push(currentType);
  });

  return result.filter(function (type, index) {
    return exclude.indexOf(index) === -1;
  });
}


function compileMap(/* lists... */) {
  var result = {
        scalar: {},
        sequence: {},
        mapping: {},
        fallback: {}
      }, index, length;

  function collectType(type) {
    result[type.kind][type.tag] = result['fallback'][type.tag] = type;
  }

  for (index = 0, length = arguments.length; index < length; index += 1) {
    arguments[index].forEach(collectType);
  }
  return result;
}


function Schema(definition) {
  this.include  = definition.include  || [];
  this.implicit = definition.implicit || [];
  this.explicit = definition.explicit || [];

  this.implicit.forEach(function (type) {
    if (type.loadKind && type.loadKind !== 'scalar') {
      throw new YAMLException('There is a non-scalar type in the implicit list of a schema. Implicit resolving of such types is not supported.');
    }
  });

  this.compiledImplicit = compileList(this, 'implicit', []);
  this.compiledExplicit = compileList(this, 'explicit', []);
  this.compiledTypeMap  = compileMap(this.compiledImplicit, this.compiledExplicit);
}


Schema.DEFAULT = null;


Schema.create = function createSchema() {
  var schemas, types;

  switch (arguments.length) {
    case 1:
      schemas = Schema.DEFAULT;
      types = arguments[0];
      break;

    case 2:
      schemas = arguments[0];
      types = arguments[1];
      break;

    default:
      throw new YAMLException('Wrong number of arguments for Schema.create function');
  }

  schemas = common.toArray(schemas);
  types = common.toArray(types);

  if (!schemas.every(function (schema) { return schema instanceof Schema; })) {
    throw new YAMLException('Specified list of super schemas (or a single Schema object) contains a non-Schema object.');
  }

  if (!types.every(function (type) { return type instanceof Type; })) {
    throw new YAMLException('Specified list of YAML types (or a single Type object) contains a non-Type object.');
  }

  return new Schema({
    include: schemas,
    explicit: types
  });
};


module.exports = Schema;

},{"./common":3,"./exception":5,"./type":14}],9:[function(require,module,exports){
// Standard YAML's Core schema.
// http://www.yaml.org/spec/1.2/spec.html#id2804923
//
// NOTE: JS-YAML does not support schema-specific tag resolution restrictions.
// So, Core schema has no distinctions from JSON schema is JS-YAML.


'use strict';


var Schema = require('../schema');


module.exports = new Schema({
  include: [
    require('./json')
  ]
});

},{"../schema":8,"./json":13}],10:[function(require,module,exports){
// JS-YAML's default schema for `load` function.
// It is not described in the YAML specification.
//
// This schema is based on JS-YAML's default safe schema and includes
// JavaScript-specific types: !!js/undefined, !!js/regexp and !!js/function.
//
// Also this schema is used as default base schema at `Schema.create` function.


'use strict';


var Schema = require('../schema');


module.exports = Schema.DEFAULT = new Schema({
  include: [
    require('./default_safe')
  ],
  explicit: [
    require('../type/js/undefined'),
    require('../type/js/regexp'),
    require('../type/js/function')
  ]
});

},{"../schema":8,"../type/js/function":19,"../type/js/regexp":20,"../type/js/undefined":21,"./default_safe":11}],11:[function(require,module,exports){
// JS-YAML's default schema for `safeLoad` function.
// It is not described in the YAML specification.
//
// This schema is based on standard YAML's Core schema and includes most of
// extra types described at YAML tag repository. (http://yaml.org/type/)


'use strict';


var Schema = require('../schema');


module.exports = new Schema({
  include: [
    require('./core')
  ],
  implicit: [
    require('../type/timestamp'),
    require('../type/merge')
  ],
  explicit: [
    require('../type/binary'),
    require('../type/omap'),
    require('../type/pairs'),
    require('../type/set')
  ]
});

},{"../schema":8,"../type/binary":15,"../type/merge":23,"../type/omap":25,"../type/pairs":26,"../type/set":28,"../type/timestamp":30,"./core":9}],12:[function(require,module,exports){
// Standard YAML's Failsafe schema.
// http://www.yaml.org/spec/1.2/spec.html#id2802346


'use strict';


var Schema = require('../schema');


module.exports = new Schema({
  explicit: [
    require('../type/str'),
    require('../type/seq'),
    require('../type/map')
  ]
});

},{"../schema":8,"../type/map":22,"../type/seq":27,"../type/str":29}],13:[function(require,module,exports){
// Standard YAML's JSON schema.
// http://www.yaml.org/spec/1.2/spec.html#id2803231
//
// NOTE: JS-YAML does not support schema-specific tag resolution restrictions.
// So, this schema is not such strict as defined in the YAML specification.
// It allows numbers in binary notaion, use `Null` and `NULL` as `null`, etc.


'use strict';


var Schema = require('../schema');


module.exports = new Schema({
  include: [
    require('./failsafe')
  ],
  implicit: [
    require('../type/null'),
    require('../type/bool'),
    require('../type/int'),
    require('../type/float')
  ]
});

},{"../schema":8,"../type/bool":16,"../type/float":17,"../type/int":18,"../type/null":24,"./failsafe":12}],14:[function(require,module,exports){
'use strict';

var YAMLException = require('./exception');

var TYPE_CONSTRUCTOR_OPTIONS = [
  'kind',
  'resolve',
  'construct',
  'instanceOf',
  'predicate',
  'represent',
  'defaultStyle',
  'styleAliases'
];

var YAML_NODE_KINDS = [
  'scalar',
  'sequence',
  'mapping'
];

function compileStyleAliases(map) {
  var result = {};

  if (map !== null) {
    Object.keys(map).forEach(function (style) {
      map[style].forEach(function (alias) {
        result[String(alias)] = style;
      });
    });
  }

  return result;
}

function Type(tag, options) {
  options = options || {};

  Object.keys(options).forEach(function (name) {
    if (TYPE_CONSTRUCTOR_OPTIONS.indexOf(name) === -1) {
      throw new YAMLException('Unknown option "' + name + '" is met in definition of "' + tag + '" YAML type.');
    }
  });

  // TODO: Add tag format check.
  this.tag          = tag;
  this.kind         = options['kind']         || null;
  this.resolve      = options['resolve']      || function () { return true; };
  this.construct    = options['construct']    || function (data) { return data; };
  this.instanceOf   = options['instanceOf']   || null;
  this.predicate    = options['predicate']    || null;
  this.represent    = options['represent']    || null;
  this.defaultStyle = options['defaultStyle'] || null;
  this.styleAliases = compileStyleAliases(options['styleAliases'] || null);

  if (YAML_NODE_KINDS.indexOf(this.kind) === -1) {
    throw new YAMLException('Unknown kind "' + this.kind + '" is specified for "' + tag + '" YAML type.');
  }
}

module.exports = Type;

},{"./exception":5}],15:[function(require,module,exports){
'use strict';

/*eslint-disable no-bitwise*/

var NodeBuffer;

try {
  // A trick for browserified version, to not include `Buffer` shim
  var _require = require;
  NodeBuffer = _require('buffer').Buffer;
} catch (__) {}

var Type       = require('../type');


// [ 64, 65, 66 ] -> [ padding, CR, LF ]
var BASE64_MAP = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\n\r';


function resolveYamlBinary(data) {
  if (data === null) return false;

  var code, idx, bitlen = 0, max = data.length, map = BASE64_MAP;

  // Convert one by one.
  for (idx = 0; idx < max; idx++) {
    code = map.indexOf(data.charAt(idx));

    // Skip CR/LF
    if (code > 64) continue;

    // Fail on illegal characters
    if (code < 0) return false;

    bitlen += 6;
  }

  // If there are any bits left, source was corrupted
  return (bitlen % 8) === 0;
}

function constructYamlBinary(data) {
  var idx, tailbits,
      input = data.replace(/[\r\n=]/g, ''), // remove CR/LF & padding to simplify scan
      max = input.length,
      map = BASE64_MAP,
      bits = 0,
      result = [];

  // Collect by 6*4 bits (3 bytes)

  for (idx = 0; idx < max; idx++) {
    if ((idx % 4 === 0) && idx) {
      result.push((bits >> 16) & 0xFF);
      result.push((bits >> 8) & 0xFF);
      result.push(bits & 0xFF);
    }

    bits = (bits << 6) | map.indexOf(input.charAt(idx));
  }

  // Dump tail

  tailbits = (max % 4) * 6;

  if (tailbits === 0) {
    result.push((bits >> 16) & 0xFF);
    result.push((bits >> 8) & 0xFF);
    result.push(bits & 0xFF);
  } else if (tailbits === 18) {
    result.push((bits >> 10) & 0xFF);
    result.push((bits >> 2) & 0xFF);
  } else if (tailbits === 12) {
    result.push((bits >> 4) & 0xFF);
  }

  // Wrap into Buffer for NodeJS and leave Array for browser
  if (NodeBuffer) return new NodeBuffer(result);

  return result;
}

function representYamlBinary(object /*, style*/) {
  var result = '', bits = 0, idx, tail,
      max = object.length,
      map = BASE64_MAP;

  // Convert every three bytes to 4 ASCII characters.

  for (idx = 0; idx < max; idx++) {
    if ((idx % 3 === 0) && idx) {
      result += map[(bits >> 18) & 0x3F];
      result += map[(bits >> 12) & 0x3F];
      result += map[(bits >> 6) & 0x3F];
      result += map[bits & 0x3F];
    }

    bits = (bits << 8) + object[idx];
  }

  // Dump tail

  tail = max % 3;

  if (tail === 0) {
    result += map[(bits >> 18) & 0x3F];
    result += map[(bits >> 12) & 0x3F];
    result += map[(bits >> 6) & 0x3F];
    result += map[bits & 0x3F];
  } else if (tail === 2) {
    result += map[(bits >> 10) & 0x3F];
    result += map[(bits >> 4) & 0x3F];
    result += map[(bits << 2) & 0x3F];
    result += map[64];
  } else if (tail === 1) {
    result += map[(bits >> 2) & 0x3F];
    result += map[(bits << 4) & 0x3F];
    result += map[64];
    result += map[64];
  }

  return result;
}

function isBinary(object) {
  return NodeBuffer && NodeBuffer.isBuffer(object);
}

module.exports = new Type('tag:yaml.org,2002:binary', {
  kind: 'scalar',
  resolve: resolveYamlBinary,
  construct: constructYamlBinary,
  predicate: isBinary,
  represent: representYamlBinary
});

},{"../type":14}],16:[function(require,module,exports){
'use strict';

var Type = require('../type');

function resolveYamlBoolean(data) {
  if (data === null) return false;

  var max = data.length;

  return (max === 4 && (data === 'true' || data === 'True' || data === 'TRUE')) ||
         (max === 5 && (data === 'false' || data === 'False' || data === 'FALSE'));
}

function constructYamlBoolean(data) {
  return data === 'true' ||
         data === 'True' ||
         data === 'TRUE';
}

function isBoolean(object) {
  return Object.prototype.toString.call(object) === '[object Boolean]';
}

module.exports = new Type('tag:yaml.org,2002:bool', {
  kind: 'scalar',
  resolve: resolveYamlBoolean,
  construct: constructYamlBoolean,
  predicate: isBoolean,
  represent: {
    lowercase: function (object) { return object ? 'true' : 'false'; },
    uppercase: function (object) { return object ? 'TRUE' : 'FALSE'; },
    camelcase: function (object) { return object ? 'True' : 'False'; }
  },
  defaultStyle: 'lowercase'
});

},{"../type":14}],17:[function(require,module,exports){
'use strict';

var common = require('../common');
var Type   = require('../type');

var YAML_FLOAT_PATTERN = new RegExp(
  '^(?:[-+]?(?:[0-9][0-9_]*)\\.[0-9_]*(?:[eE][-+][0-9]+)?' +
  '|\\.[0-9_]+(?:[eE][-+][0-9]+)?' +
  '|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\\.[0-9_]*' +
  '|[-+]?\\.(?:inf|Inf|INF)' +
  '|\\.(?:nan|NaN|NAN))$');

function resolveYamlFloat(data) {
  if (data === null) return false;

  if (!YAML_FLOAT_PATTERN.test(data)) return false;

  return true;
}

function constructYamlFloat(data) {
  var value, sign, base, digits;

  value  = data.replace(/_/g, '').toLowerCase();
  sign   = value[0] === '-' ? -1 : 1;
  digits = [];

  if ('+-'.indexOf(value[0]) >= 0) {
    value = value.slice(1);
  }

  if (value === '.inf') {
    return (sign === 1) ? Number.POSITIVE_INFINITY : Number.NEGATIVE_INFINITY;

  } else if (value === '.nan') {
    return NaN;

  } else if (value.indexOf(':') >= 0) {
    value.split(':').forEach(function (v) {
      digits.unshift(parseFloat(v, 10));
    });

    value = 0.0;
    base = 1;

    digits.forEach(function (d) {
      value += d * base;
      base *= 60;
    });

    return sign * value;

  }
  return sign * parseFloat(value, 10);
}


var SCIENTIFIC_WITHOUT_DOT = /^[-+]?[0-9]+e/;

function representYamlFloat(object, style) {
  var res;

  if (isNaN(object)) {
    switch (style) {
      case 'lowercase': return '.nan';
      case 'uppercase': return '.NAN';
      case 'camelcase': return '.NaN';
    }
  } else if (Number.POSITIVE_INFINITY === object) {
    switch (style) {
      case 'lowercase': return '.inf';
      case 'uppercase': return '.INF';
      case 'camelcase': return '.Inf';
    }
  } else if (Number.NEGATIVE_INFINITY === object) {
    switch (style) {
      case 'lowercase': return '-.inf';
      case 'uppercase': return '-.INF';
      case 'camelcase': return '-.Inf';
    }
  } else if (common.isNegativeZero(object)) {
    return '-0.0';
  }

  res = object.toString(10);

  // JS stringifier can build scientific format without dots: 5e-100,
  // while YAML requres dot: 5.e-100. Fix it with simple hack

  return SCIENTIFIC_WITHOUT_DOT.test(res) ? res.replace('e', '.e') : res;
}

function isFloat(object) {
  return (Object.prototype.toString.call(object) === '[object Number]') &&
         (object % 1 !== 0 || common.isNegativeZero(object));
}

module.exports = new Type('tag:yaml.org,2002:float', {
  kind: 'scalar',
  resolve: resolveYamlFloat,
  construct: constructYamlFloat,
  predicate: isFloat,
  represent: representYamlFloat,
  defaultStyle: 'lowercase'
});

},{"../common":3,"../type":14}],18:[function(require,module,exports){
'use strict';

var common = require('../common');
var Type   = require('../type');

function isHexCode(c) {
  return ((0x30/* 0 */ <= c) && (c <= 0x39/* 9 */)) ||
         ((0x41/* A */ <= c) && (c <= 0x46/* F */)) ||
         ((0x61/* a */ <= c) && (c <= 0x66/* f */));
}

function isOctCode(c) {
  return ((0x30/* 0 */ <= c) && (c <= 0x37/* 7 */));
}

function isDecCode(c) {
  return ((0x30/* 0 */ <= c) && (c <= 0x39/* 9 */));
}

function resolveYamlInteger(data) {
  if (data === null) return false;

  var max = data.length,
      index = 0,
      hasDigits = false,
      ch;

  if (!max) return false;

  ch = data[index];

  // sign
  if (ch === '-' || ch === '+') {
    ch = data[++index];
  }

  if (ch === '0') {
    // 0
    if (index + 1 === max) return true;
    ch = data[++index];

    // base 2, base 8, base 16

    if (ch === 'b') {
      // base 2
      index++;

      for (; index < max; index++) {
        ch = data[index];
        if (ch === '_') continue;
        if (ch !== '0' && ch !== '1') return false;
        hasDigits = true;
      }
      return hasDigits;
    }


    if (ch === 'x') {
      // base 16
      index++;

      for (; index < max; index++) {
        ch = data[index];
        if (ch === '_') continue;
        if (!isHexCode(data.charCodeAt(index))) return false;
        hasDigits = true;
      }
      return hasDigits;
    }

    // base 8
    for (; index < max; index++) {
      ch = data[index];
      if (ch === '_') continue;
      if (!isOctCode(data.charCodeAt(index))) return false;
      hasDigits = true;
    }
    return hasDigits;
  }

  // base 10 (except 0) or base 60

  for (; index < max; index++) {
    ch = data[index];
    if (ch === '_') continue;
    if (ch === ':') break;
    if (!isDecCode(data.charCodeAt(index))) {
      return false;
    }
    hasDigits = true;
  }

  if (!hasDigits) return false;

  // if !base60 - done;
  if (ch !== ':') return true;

  // base60 almost not used, no needs to optimize
  return /^(:[0-5]?[0-9])+$/.test(data.slice(index));
}

function constructYamlInteger(data) {
  var value = data, sign = 1, ch, base, digits = [];

  if (value.indexOf('_') !== -1) {
    value = value.replace(/_/g, '');
  }

  ch = value[0];

  if (ch === '-' || ch === '+') {
    if (ch === '-') sign = -1;
    value = value.slice(1);
    ch = value[0];
  }

  if (value === '0') return 0;

  if (ch === '0') {
    if (value[1] === 'b') return sign * parseInt(value.slice(2), 2);
    if (value[1] === 'x') return sign * parseInt(value, 16);
    return sign * parseInt(value, 8);
  }

  if (value.indexOf(':') !== -1) {
    value.split(':').forEach(function (v) {
      digits.unshift(parseInt(v, 10));
    });

    value = 0;
    base = 1;

    digits.forEach(function (d) {
      value += (d * base);
      base *= 60;
    });

    return sign * value;

  }

  return sign * parseInt(value, 10);
}

function isInteger(object) {
  return (Object.prototype.toString.call(object)) === '[object Number]' &&
         (object % 1 === 0 && !common.isNegativeZero(object));
}

module.exports = new Type('tag:yaml.org,2002:int', {
  kind: 'scalar',
  resolve: resolveYamlInteger,
  construct: constructYamlInteger,
  predicate: isInteger,
  represent: {
    binary:      function (object) { return '0b' + object.toString(2); },
    octal:       function (object) { return '0'  + object.toString(8); },
    decimal:     function (object) { return        object.toString(10); },
    hexadecimal: function (object) { return '0x' + object.toString(16).toUpperCase(); }
  },
  defaultStyle: 'decimal',
  styleAliases: {
    binary:      [ 2,  'bin' ],
    octal:       [ 8,  'oct' ],
    decimal:     [ 10, 'dec' ],
    hexadecimal: [ 16, 'hex' ]
  }
});

},{"../common":3,"../type":14}],19:[function(require,module,exports){
'use strict';

var esprima;

// Browserified version does not have esprima
//
// 1. For node.js just require module as deps
// 2. For browser try to require mudule via external AMD system.
//    If not found - try to fallback to window.esprima. If not
//    found too - then fail to parse.
//
try {
  // workaround to exclude package from browserify list.
  var _require = require;
  esprima = _require('esprima');
} catch (_) {
  /*global window */
  if (typeof window !== 'undefined') esprima = window.esprima;
}

var Type = require('../../type');

function resolveJavascriptFunction(data) {
  if (data === null) return false;

  try {
    var source = '(' + data + ')',
        ast    = esprima.parse(source, { range: true });

    if (ast.type                    !== 'Program'             ||
        ast.body.length             !== 1                     ||
        ast.body[0].type            !== 'ExpressionStatement' ||
        ast.body[0].expression.type !== 'FunctionExpression') {
      return false;
    }

    return true;
  } catch (err) {
    return false;
  }
}

function constructJavascriptFunction(data) {
  /*jslint evil:true*/

  var source = '(' + data + ')',
      ast    = esprima.parse(source, { range: true }),
      params = [],
      body;

  if (ast.type                    !== 'Program'             ||
      ast.body.length             !== 1                     ||
      ast.body[0].type            !== 'ExpressionStatement' ||
      ast.body[0].expression.type !== 'FunctionExpression') {
    throw new Error('Failed to resolve function');
  }

  ast.body[0].expression.params.forEach(function (param) {
    params.push(param.name);
  });

  body = ast.body[0].expression.body.range;

  // Esprima's ranges include the first '{' and the last '}' characters on
  // function expressions. So cut them out.
  /*eslint-disable no-new-func*/
  return new Function(params, source.slice(body[0] + 1, body[1] - 1));
}

function representJavascriptFunction(object /*, style*/) {
  return object.toString();
}

function isFunction(object) {
  return Object.prototype.toString.call(object) === '[object Function]';
}

module.exports = new Type('tag:yaml.org,2002:js/function', {
  kind: 'scalar',
  resolve: resolveJavascriptFunction,
  construct: constructJavascriptFunction,
  predicate: isFunction,
  represent: representJavascriptFunction
});

},{"../../type":14}],20:[function(require,module,exports){
'use strict';

var Type = require('../../type');

function resolveJavascriptRegExp(data) {
  if (data === null) return false;
  if (data.length === 0) return false;

  var regexp = data,
      tail   = /\/([gim]*)$/.exec(data),
      modifiers = '';

  // if regexp starts with '/' it can have modifiers and must be properly closed
  // `/foo/gim` - modifiers tail can be maximum 3 chars
  if (regexp[0] === '/') {
    if (tail) modifiers = tail[1];

    if (modifiers.length > 3) return false;
    // if expression starts with /, is should be properly terminated
    if (regexp[regexp.length - modifiers.length - 1] !== '/') return false;
  }

  return true;
}

function constructJavascriptRegExp(data) {
  var regexp = data,
      tail   = /\/([gim]*)$/.exec(data),
      modifiers = '';

  // `/foo/gim` - tail can be maximum 4 chars
  if (regexp[0] === '/') {
    if (tail) modifiers = tail[1];
    regexp = regexp.slice(1, regexp.length - modifiers.length - 1);
  }

  return new RegExp(regexp, modifiers);
}

function representJavascriptRegExp(object /*, style*/) {
  var result = '/' + object.source + '/';

  if (object.global) result += 'g';
  if (object.multiline) result += 'm';
  if (object.ignoreCase) result += 'i';

  return result;
}

function isRegExp(object) {
  return Object.prototype.toString.call(object) === '[object RegExp]';
}

module.exports = new Type('tag:yaml.org,2002:js/regexp', {
  kind: 'scalar',
  resolve: resolveJavascriptRegExp,
  construct: constructJavascriptRegExp,
  predicate: isRegExp,
  represent: representJavascriptRegExp
});

},{"../../type":14}],21:[function(require,module,exports){
'use strict';

var Type = require('../../type');

function resolveJavascriptUndefined() {
  return true;
}

function constructJavascriptUndefined() {
  /*eslint-disable no-undefined*/
  return undefined;
}

function representJavascriptUndefined() {
  return '';
}

function isUndefined(object) {
  return typeof object === 'undefined';
}

module.exports = new Type('tag:yaml.org,2002:js/undefined', {
  kind: 'scalar',
  resolve: resolveJavascriptUndefined,
  construct: constructJavascriptUndefined,
  predicate: isUndefined,
  represent: representJavascriptUndefined
});

},{"../../type":14}],22:[function(require,module,exports){
'use strict';

var Type = require('../type');

module.exports = new Type('tag:yaml.org,2002:map', {
  kind: 'mapping',
  construct: function (data) { return data !== null ? data : {}; }
});

},{"../type":14}],23:[function(require,module,exports){
'use strict';

var Type = require('../type');

function resolveYamlMerge(data) {
  return data === '<<' || data === null;
}

module.exports = new Type('tag:yaml.org,2002:merge', {
  kind: 'scalar',
  resolve: resolveYamlMerge
});

},{"../type":14}],24:[function(require,module,exports){
'use strict';

var Type = require('../type');

function resolveYamlNull(data) {
  if (data === null) return true;

  var max = data.length;

  return (max === 1 && data === '~') ||
         (max === 4 && (data === 'null' || data === 'Null' || data === 'NULL'));
}

function constructYamlNull() {
  return null;
}

function isNull(object) {
  return object === null;
}

module.exports = new Type('tag:yaml.org,2002:null', {
  kind: 'scalar',
  resolve: resolveYamlNull,
  construct: constructYamlNull,
  predicate: isNull,
  represent: {
    canonical: function () { return '~';    },
    lowercase: function () { return 'null'; },
    uppercase: function () { return 'NULL'; },
    camelcase: function () { return 'Null'; }
  },
  defaultStyle: 'lowercase'
});

},{"../type":14}],25:[function(require,module,exports){
'use strict';

var Type = require('../type');

var _hasOwnProperty = Object.prototype.hasOwnProperty;
var _toString       = Object.prototype.toString;

function resolveYamlOmap(data) {
  if (data === null) return true;

  var objectKeys = [], index, length, pair, pairKey, pairHasKey,
      object = data;

  for (index = 0, length = object.length; index < length; index += 1) {
    pair = object[index];
    pairHasKey = false;

    if (_toString.call(pair) !== '[object Object]') return false;

    for (pairKey in pair) {
      if (_hasOwnProperty.call(pair, pairKey)) {
        if (!pairHasKey) pairHasKey = true;
        else return false;
      }
    }

    if (!pairHasKey) return false;

    if (objectKeys.indexOf(pairKey) === -1) objectKeys.push(pairKey);
    else return false;
  }

  return true;
}

function constructYamlOmap(data) {
  return data !== null ? data : [];
}

module.exports = new Type('tag:yaml.org,2002:omap', {
  kind: 'sequence',
  resolve: resolveYamlOmap,
  construct: constructYamlOmap
});

},{"../type":14}],26:[function(require,module,exports){
'use strict';

var Type = require('../type');

var _toString = Object.prototype.toString;

function resolveYamlPairs(data) {
  if (data === null) return true;

  var index, length, pair, keys, result,
      object = data;

  result = new Array(object.length);

  for (index = 0, length = object.length; index < length; index += 1) {
    pair = object[index];

    if (_toString.call(pair) !== '[object Object]') return false;

    keys = Object.keys(pair);

    if (keys.length !== 1) return false;

    result[index] = [ keys[0], pair[keys[0]] ];
  }

  return true;
}

function constructYamlPairs(data) {
  if (data === null) return [];

  var index, length, pair, keys, result,
      object = data;

  result = new Array(object.length);

  for (index = 0, length = object.length; index < length; index += 1) {
    pair = object[index];

    keys = Object.keys(pair);

    result[index] = [ keys[0], pair[keys[0]] ];
  }

  return result;
}

module.exports = new Type('tag:yaml.org,2002:pairs', {
  kind: 'sequence',
  resolve: resolveYamlPairs,
  construct: constructYamlPairs
});

},{"../type":14}],27:[function(require,module,exports){
'use strict';

var Type = require('../type');

module.exports = new Type('tag:yaml.org,2002:seq', {
  kind: 'sequence',
  construct: function (data) { return data !== null ? data : []; }
});

},{"../type":14}],28:[function(require,module,exports){
'use strict';

var Type = require('../type');

var _hasOwnProperty = Object.prototype.hasOwnProperty;

function resolveYamlSet(data) {
  if (data === null) return true;

  var key, object = data;

  for (key in object) {
    if (_hasOwnProperty.call(object, key)) {
      if (object[key] !== null) return false;
    }
  }

  return true;
}

function constructYamlSet(data) {
  return data !== null ? data : {};
}

module.exports = new Type('tag:yaml.org,2002:set', {
  kind: 'mapping',
  resolve: resolveYamlSet,
  construct: constructYamlSet
});

},{"../type":14}],29:[function(require,module,exports){
'use strict';

var Type = require('../type');

module.exports = new Type('tag:yaml.org,2002:str', {
  kind: 'scalar',
  construct: function (data) { return data !== null ? data : ''; }
});

},{"../type":14}],30:[function(require,module,exports){
'use strict';

var Type = require('../type');

var YAML_DATE_REGEXP = new RegExp(
  '^([0-9][0-9][0-9][0-9])'          + // [1] year
  '-([0-9][0-9])'                    + // [2] month
  '-([0-9][0-9])$');                   // [3] day

var YAML_TIMESTAMP_REGEXP = new RegExp(
  '^([0-9][0-9][0-9][0-9])'          + // [1] year
  '-([0-9][0-9]?)'                   + // [2] month
  '-([0-9][0-9]?)'                   + // [3] day
  '(?:[Tt]|[ \\t]+)'                 + // ...
  '([0-9][0-9]?)'                    + // [4] hour
  ':([0-9][0-9])'                    + // [5] minute
  ':([0-9][0-9])'                    + // [6] second
  '(?:\\.([0-9]*))?'                 + // [7] fraction
  '(?:[ \\t]*(Z|([-+])([0-9][0-9]?)' + // [8] tz [9] tz_sign [10] tz_hour
  '(?::([0-9][0-9]))?))?$');           // [11] tz_minute

function resolveYamlTimestamp(data) {
  if (data === null) return false;
  if (YAML_DATE_REGEXP.exec(data) !== null) return true;
  if (YAML_TIMESTAMP_REGEXP.exec(data) !== null) return true;
  return false;
}

function constructYamlTimestamp(data) {
  var match, year, month, day, hour, minute, second, fraction = 0,
      delta = null, tz_hour, tz_minute, date;

  match = YAML_DATE_REGEXP.exec(data);
  if (match === null) match = YAML_TIMESTAMP_REGEXP.exec(data);

  if (match === null) throw new Error('Date resolve error');

  // match: [1] year [2] month [3] day

  year = +(match[1]);
  month = +(match[2]) - 1; // JS month starts with 0
  day = +(match[3]);

  if (!match[4]) { // no hour
    return new Date(Date.UTC(year, month, day));
  }

  // match: [4] hour [5] minute [6] second [7] fraction

  hour = +(match[4]);
  minute = +(match[5]);
  second = +(match[6]);

  if (match[7]) {
    fraction = match[7].slice(0, 3);
    while (fraction.length < 3) { // milli-seconds
      fraction += '0';
    }
    fraction = +fraction;
  }

  // match: [8] tz [9] tz_sign [10] tz_hour [11] tz_minute

  if (match[9]) {
    tz_hour = +(match[10]);
    tz_minute = +(match[11] || 0);
    delta = (tz_hour * 60 + tz_minute) * 60000; // delta in mili-seconds
    if (match[9] === '-') delta = -delta;
  }

  date = new Date(Date.UTC(year, month, day, hour, minute, second, fraction));

  if (delta) date.setTime(date.getTime() - delta);

  return date;
}

function representYamlTimestamp(object /*, style*/) {
  return object.toISOString();
}

module.exports = new Type('tag:yaml.org,2002:timestamp', {
  kind: 'scalar',
  resolve: resolveYamlTimestamp,
  construct: constructYamlTimestamp,
  instanceOf: Date,
  represent: representYamlTimestamp
});

},{"../type":14}],31:[function(require,module,exports){
(function (global){
var parse = require("json-to-ast");

var reconstructNode = function (node, lexicalInfo) {
    var acc = {};
    node.children.forEach(function (child) {
        acc[child.key.value] = reconstruct(child.value);
    });
    acc['__location__'] = lexicalInfo;
    return acc;
};

var reconstructLiteral = function (node) {
    return node.value;
};

var reconstructArray = function (node) {
    var acc = [];
    node.children.forEach(function (child) {
        acc.push(reconstruct(child));
    });
    return acc;
};

var reconstruct = function (node) {
    var location = node.loc;
    var lexicalInfo = null;
    if (location != null) {
        lexicalInfo = {
            "start-line": location.start.line,
            "start-column": location.start.column,
            "start-index": -1,
            "end-line": location.end.line,
            "end-column": location.end.column,
            "end-index": -1
        };
    }

    switch (node.type) {
        case "object": return reconstructNode(node, lexicalInfo);
        case "literal": return reconstructLiteral(node);
        case "array": return reconstructArray(node);
    }
};

var ast = function (file, text) {
    var tree = parse(text, { source: file });
    return reconstruct(tree);
};


module.exports = ast;
global.JS_AST = ast;

//console.log(JSON.stringify(ast("test.yaml", '{"a":1,"b":[{"b":3,"d":{"e":3}}, true, false]}'), null, 2));

}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"json-to-ast":33}],32:[function(require,module,exports){
(function (global){
var fs = (typeof (NODE_FS) === 'undefined' ? { readFile: function () { throw new Error("readFile only supported in node.js") } } : NODE_FS);
var yaml = require('./js-yaml/index.js');
var path = require("path");
var rest = require("rest");
var json_ast = require("./json_ast");

global.FRAGMENTS_CACHE = {};
global.PENDING_LIBRARIES = [];

var ensureFileUri = function (uri) {
    if (uri.indexOf("://") === -1) {
        uri = "file://" + uri;
    }
    return uri;
};

var startsWith = function (s, p) {
    return s.indexOf(p) === 0;
};

var isLocalFile = function (location) {
    return (startsWith(location, "file://") || location.indexOf("://") === -1);
};

var inCache = function (location, cacheDirs) {
    for (var domain in cacheDirs || {}) {
        if (startsWith(location, domain)) {
            return location.replace(domain, cacheDirs[domain]);
        }
    }
};

var resolveFile = function (location, cb) {
    if (isLocalFile(location)) {
        fs.readFile(location.replace("file://", ""), function (err, data) {
            if (err) {
                cb(err);
            } else {
                cb(null, data.toString());
            }
        });
    } else if (inCache(location, global.PARSING_OPTIONS.cacheDirs || {})) {
        resolveFile(inCache(location, global.PARSING_OPTIONS.cacheDirs || {}), cb);
    } else {
        global.JS_REST(location).then(function (response) {
            cb(null, response.entity);
        }).catch(function (err) {
            cb(err);
        });
    }
};

var resolvePath = function (location, path) {
    var lastComponent = location.split("/").pop();
    var base = location.replace(lastComponent, "");
    if (path.indexOf("/") === 0 || path.indexOf("://") !== -1) {
        //console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + path);
        return path;
    } else {
        //console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + (base + path));
        return base + path;
    }
};

var updateFragments = function (file, data, pending, cb) {
    var matches = data.match(/!include\s+(.+)/g) || [];
    var files = matches.map(function (f) {
        var filePath = f.split(/!include\s+/)[1];
        filePath = filePath.replace(/'|"$/, "");
        var location = resolvePath(file.location, filePath);
        return {
            "path": filePath,
            "location": location
        };
    });
    pending = pending.concat(files);

    FRAGMENTS_CACHE[file.path || file.location] = {
        "data": data,
        "location": file.location
    };

    if (pending.length === 0) {
        cb(null, FRAGMENTS_CACHE);
    } else {
        var next = pending.shift();
        cacheFragments(next, cb, pending);
    }
};

var cacheFragments = function (fileOrData, cb, pending) {
    if (pending == null) {
        pending = [];
    }

    if (fileOrData.data != null) {
        updateFragments(fileOrData, fileOrData.data, pending, cb);
    } else {
        // console.log(fileOrData);
        if (typeof (AMF_LOADING_EVENT) !== 'undefined') {
            try {
                AMF_LOADING_EVENT(fileOrData.location);
            } catch (e) { }
        }
        resolveFile(fileOrData.location, function (err, data) {
            if (err) {
                cb(err);
            } else {
                updateFragments(fileOrData, data, pending, cb);
            }
        });
    }

};

var loadLibraries = function (loaded, cb, pending) {
    if (pending == null) {
        pending = PENDING_LIBRARIES;
        PENDING_LIBRARIES = [];
        loadLibraries(loaded, cb, pending);
    } else {
        if (pending.length == 0) {
            cb(null, loaded);
        } else {
            var next = pending.shift();
            parseYamlFile(next.location, global.PARSING_OPTIONS, function (err, loadedFragment) {
                if (err) {
                    cb(err, loaded);
                } else {
                    loaded.uses[next.alias] = loadedFragment;
                    loadLibraries(loaded, cb, pending);
                }
            });
        }
    }
};

var getFragmentInfo = function (fragment) {
    var fragmentInfo = fragment.data.split("\n")[0];
    if (fragmentInfo.indexOf("#%RAML") === 0) {
        return fragmentInfo;

    } else {
        return null;
    }

};

var collectLibraries = function (fragment, location) {
    var libraries = fragment.uses || {};
    for (var p in libraries) {
        if (p !== "__location__") {
            var resolvedLocation = resolvePath(location, libraries[p]);
            PENDING_LIBRARIES.push({
                "path": libraries[p],
                "location": resolvedLocation,
                "alias": p
            });
        }
    }
};

var FragmentType = new yaml.Type("!include", {
    kind: "scalar",

    resolve: function (file) {
        return FRAGMENTS_CACHE[file] != null;
    },

    construct: function (file) {
        var fragment = FRAGMENTS_CACHE[file];
        var parsed = null;
        // console.log("Trying to parse inner '" + fragment.data.substr(0, 10) + "'");

        try {
            parsed = yaml.load(fragment.data, { schema: FRAGMENT_SCHEMA });
        } catch (e) {
            parsed = fragment.data;
        }
        var fragmentInfo = getFragmentInfo(fragment);

        var location = fragment.location;

        return {
            "@location": ensureFileUri(location),
            "@fragment": fragmentInfo,
            "@data": parsed,
            "@raw": fragment.data
        };
    }
});

var FRAGMENT_SCHEMA = yaml.Schema.create([FragmentType]);

var parseYamlFile = function (location, options, cb) {
    global.PARSING_OPTIONS = options || {};
    cacheFragments({ "location": location }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            collectLibraries(loaded, location);

            loadLibraries(loaded, function (err, loaded) {
                var result = { "@data": loaded };
                result["@location"] = ensureFileUri(location);
                result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
                result["@raw"] = FRAGMENTS_CACHE[location].data;
                cb(null, result);
            });
        } catch (e) {
            cb(e);
        }
    });
};

var parseYamlString = function (location, data, options, cb) {
    global.PARSING_OPTIONS = options || {};
    cacheFragments({ "location": location, "data": data }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            collectLibraries(loaded, location);
            loadLibraries(loaded, function (err, loaded) {
                if (err == null) {
                    var result = { "@data": loaded };
                    result["@location"] = ensureFileUri(location);
                    result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
                    result["@raw"] = FRAGMENTS_CACHE[location].data;
                    cb(null, result);
                } else {
                    cb(err);
                }

            });
        } catch (e) {
            cb(e);
        }
    });
};

module.exports.parseYamlFile = parseYamlFile;
module.exports.parseYamlString = parseYamlString;
module.exports.loadYaml = yaml.load;
module.exports.dump = yaml.dump;
global.JS_YAML = {};
global.JS_YAML.loadYaml = yaml.load;
global.JS_YAML.parseYamlFile = parseYamlFile;
global.JS_YAML.parseYamlString = parseYamlString;
global.JS_YAML.dump = yaml.dump;
global.JS_REST = function (location) {
    console.log("USING PROXY?");
    if (typeof (global.JS_USE_PROXY) !== "undefined") {
        console.log("YES: " + global.JS_USE_PROXY.replace("$URL", encodeURIComponent(location)));
        location = global.JS_USE_PROXY.replace("$URL", encodeURIComponent(location));
    } else {
        console.log("NOPE!");
    }
    return rest(location);
};

}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"./js-yaml/index.js":1,"./json_ast":31,"path":40,"rest":34}],33:[function(require,module,exports){
(function (global, factory) {
	if (typeof define === "function" && define.amd) {
		define(['module'], factory);
	} else if (typeof exports !== "undefined") {
		factory(module);
	} else {
		var mod = {
			exports: {}
		};
		factory(mod);
		global.parse = mod.exports;
	}
})(this, function (module) {
	'use strict';

	var _extends = Object.assign || function (target) {
		for (var i = 1; i < arguments.length; i++) {
			var source = arguments[i];

			for (var key in source) {
				if (Object.prototype.hasOwnProperty.call(source, key)) {
					target[key] = source[key];
				}
			}
		}

		return target;
	};

	function _classCallCheck(instance, Constructor) {
		if (!(instance instanceof Constructor)) {
			throw new TypeError("Cannot call a class as a function");
		}
	}

	function _possibleConstructorReturn(self, call) {
		if (!self) {
			throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
		}

		return call && (typeof call === "object" || typeof call === "function") ? call : self;
	}

	function _inherits(subClass, superClass) {
		if (typeof superClass !== "function" && superClass !== null) {
			throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
		}

		subClass.prototype = Object.create(superClass && superClass.prototype, {
			constructor: {
				value: subClass,
				enumerable: false,
				writable: true,
				configurable: true
			}
		});
		if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
	}

	var location = function location(startLine, startColumn, startOffset, endLine, endColumn, endOffset, source) {
		return {
			start: {
				line: startLine,
				column: startColumn,
				offset: startOffset
			},
			end: {
				line: endLine,
				column: endColumn,
				offset: endOffset
			},
			source: source || null
		};
	};

	function showCodeFragment(source, linePosition, columnPosition) {
		var lines = source.split(/\n|\r\n?|\f/);
		var line = lines[linePosition - 1];
		var marker = new Array(columnPosition).join(' ') + '^';

		return line + '\n' + marker;
	}

	var ParseError = function (_SyntaxError) {
		_inherits(ParseError, _SyntaxError);

		function ParseError(message, source, linePosition, columnPosition) {
			_classCallCheck(this, ParseError);

			var fullMessage = linePosition ? message + '\n' + showCodeFragment(source, linePosition, columnPosition) : message;

			var _this = _possibleConstructorReturn(this, (ParseError.__proto__ || Object.getPrototypeOf(ParseError)).call(this, fullMessage));

			_this.rawMessage = message;
			return _this;
		}

		return ParseError;
	}(SyntaxError);

	var error = function error(message, source, line, column) {
		throw new ParseError(message, source, line, column);
	};

	var parseErrorTypes = {
		unexpectedEnd: function unexpectedEnd() {
			return 'Unexpected end of JSON input';
		},
		unexpectedToken: function unexpectedToken(token, line, column) {
			return 'Unexpected token <' + token + '> at ' + line + ':' + column;
		}
	};

	var tokenizeErrorTypes = {
		cannotTokenizeSymbol: function cannotTokenizeSymbol(symbol, line, column) {
			return 'Cannot tokenize symbol <' + symbol + '> at ' + line + ':' + column;
		}
	};

	var tokenTypes = {
		LEFT_BRACE: 0, // {
		RIGHT_BRACE: 1, // }
		LEFT_BRACKET: 2, // [
		RIGHT_BRACKET: 3, // ]
		COLON: 4, // :
		COMMA: 5, // ,
		STRING: 6, //
		NUMBER: 7, //
		TRUE: 8, // true
		FALSE: 9, // false
		NULL: 10 // null
	};

	var punctuatorTokensMap = { // Lexeme: Token
		'{': tokenTypes.LEFT_BRACE,
		'}': tokenTypes.RIGHT_BRACE,
		'[': tokenTypes.LEFT_BRACKET,
		']': tokenTypes.RIGHT_BRACKET,
		':': tokenTypes.COLON,
		',': tokenTypes.COMMA
	};

	var keywordTokensMap = { // Lexeme: Token config
		'true': { type: tokenTypes.TRUE, value: true },
		'false': { type: tokenTypes.FALSE, value: false },
		'null': { type: tokenTypes.NULL, value: null }
	};

	var stringStates = {
		_START_: 0,
		START_QUOTE_OR_CHAR: 1,
		ESCAPE: 2
	};

	var escapes = {
		'"': 0, // Quotation mask
		'\\': 1, // Reverse solidus
		'/': 2, // Solidus
		'b': 3, // Backspace
		'f': 4, // Form feed
		'n': 5, // New line
		'r': 6, // Carriage return
		't': 7, // Horizontal tab
		'u': 8 // 4 hexadecimal digits
	};

	var numberStates = {
		_START_: 0,
		MINUS: 1,
		ZERO: 2,
		DIGIT: 3,
		POINT: 4,
		DIGIT_FRACTION: 5,
		EXP: 6,
		EXP_DIGIT_OR_SIGN: 7
	};

	// HELPERS

	function isDigit1to9(char) {
		return char >= '1' && char <= '9';
	}

	function isDigit(char) {
		return char >= '0' && char <= '9';
	}

	function isHex(char) {
		return isDigit(char) || char >= 'a' && char <= 'f' || char >= 'A' && char <= 'F';
	}

	function isExp(char) {
		return char === 'e' || char === 'E';
	}

	// PARSERS

	function parseWhitespace(input, index, line, column) {
		var char = input.charAt(index);

		if (char === '\r') {
			// CR (Unix)
			index++;
			line++;
			column = 1;
			if (input.charAt(index) === '\n') {
				// CRLF (Windows)
				index++;
			}
		} else if (char === '\n') {
			// LF (MacOS)
			index++;
			line++;
			column = 1;
		} else if (char === '\t' || char === ' ') {
			index++;
			column++;
		} else {
			return null;
		}

		return {
			index: index,
			line: line,
			column: column
		};
	}

	function parseChar(input, index, line, column) {
		var char = input.charAt(index);

		if (char in punctuatorTokensMap) {
			return {
				type: punctuatorTokensMap[char],
				line: line,
				column: column + 1,
				index: index + 1,
				value: null
			};
		}

		return null;
	}

	function parseKeyword(input, index, line, column) {
		for (var name in keywordTokensMap) {
			if (keywordTokensMap.hasOwnProperty(name) && input.substr(index, name.length) === name) {
				var _keywordTokensMap$nam = keywordTokensMap[name],
				    type = _keywordTokensMap$nam.type,
				    value = _keywordTokensMap$nam.value;


				return {
					type: type,
					line: line,
					column: column + name.length,
					index: index + name.length,
					value: value
				};
			}
		}

		return null;
	}

	function parseString(input, index, line, column) {
		var startIndex = index;
		var buffer = '';
		var state = stringStates._START_;

		while (index < input.length) {
			var char = input.charAt(index);

			switch (state) {
				case stringStates._START_:
					{
						if (char === '"') {
							state = stringStates.START_QUOTE_OR_CHAR;
							index++;
						} else {
							return null;
						}
						break;
					}

				case stringStates.START_QUOTE_OR_CHAR:
					{
						if (char === '\\') {
							state = stringStates.ESCAPE;
							buffer += char;
							index++;
						} else if (char === '"') {
							index++;
							return {
								type: tokenTypes.STRING,
								line: line,
								column: column + index - startIndex,
								index: index,
								value: buffer
							};
						} else {
							buffer += char;
							index++;
						}
						break;
					}

				case stringStates.ESCAPE:
					{
						if (char in escapes) {
							buffer += char;
							index++;
							if (char === 'u') {
								for (var i = 0; i < 4; i++) {
									var curChar = input.charAt(index);
									if (curChar && isHex(curChar)) {
										buffer += curChar;
										index++;
									} else {
										return null;
									}
								}
							}
							state = stringStates.START_QUOTE_OR_CHAR;
						} else {
							return null;
						}
						break;
					}
			}
		}
	}

	function parseNumber(input, index, line, column) {
		var startIndex = index;
		var passedValueIndex = index;
		var state = numberStates._START_;

		iterator: while (index < input.length) {
			var char = input.charAt(index);

			switch (state) {
				case numberStates._START_:
					{
						if (char === '-') {
							state = numberStates.MINUS;
						} else if (char === '0') {
							passedValueIndex = index + 1;
							state = numberStates.ZERO;
						} else if (isDigit1to9(char)) {
							passedValueIndex = index + 1;
							state = numberStates.DIGIT;
						} else {
							return null;
						}
						break;
					}

				case numberStates.MINUS:
					{
						if (char === '0') {
							passedValueIndex = index + 1;
							state = numberStates.ZERO;
						} else if (isDigit1to9(char)) {
							passedValueIndex = index + 1;
							state = numberStates.DIGIT;
						} else {
							return null;
						}
						break;
					}

				case numberStates.ZERO:
					{
						if (char === '.') {
							state = numberStates.POINT;
						} else if (isExp(char)) {
							state = numberStates.EXP;
						} else {
							break iterator;
						}
						break;
					}

				case numberStates.DIGIT:
					{
						if (isDigit(char)) {
							passedValueIndex = index + 1;
						} else if (char === '.') {
							state = numberStates.POINT;
						} else if (isExp(char)) {
							state = numberStates.EXP;
						} else {
							break iterator;
						}
						break;
					}

				case numberStates.POINT:
					{
						if (isDigit(char)) {
							passedValueIndex = index + 1;
							state = numberStates.DIGIT_FRACTION;
						} else {
							break iterator;
						}
						break;
					}

				case numberStates.DIGIT_FRACTION:
					{
						if (isDigit(char)) {
							passedValueIndex = index + 1;
						} else if (isExp(char)) {
							state = numberStates.EXP;
						} else {
							break iterator;
						}
						break;
					}

				case numberStates.EXP:
					{
						if (char === '+' || char === '-') {
							state = numberStates.EXP_DIGIT_OR_SIGN;
						} else if (isDigit(char)) {
							passedValueIndex = index + 1;
							state = numberStates.EXP_DIGIT_OR_SIGN;
						} else {
							break iterator;
						}
						break;
					}

				case numberStates.EXP_DIGIT_OR_SIGN:
					{
						if (isDigit(char)) {
							passedValueIndex = index + 1;
						} else {
							break iterator;
						}
						break;
					}
			}

			index++;
		}

		if (passedValueIndex > 0) {
			return {
				type: tokenTypes.NUMBER,
				line: line,
				column: column + passedValueIndex - startIndex,
				index: passedValueIndex,
				value: parseFloat(input.substring(startIndex, passedValueIndex))
			};
		}

		return null;
	}

	function tokenize(input, settings) {
		var line = 1;
		var column = 1;
		var index = 0;
		var tokens = [];

		while (index < input.length) {
			var args = [input, index, line, column];
			var whitespace = parseWhitespace.apply(undefined, args);

			if (whitespace) {
				index = whitespace.index;
				line = whitespace.line;
				column = whitespace.column;
				continue;
			}

			var matched = parseChar.apply(undefined, args) || parseKeyword.apply(undefined, args) || parseString.apply(undefined, args) || parseNumber.apply(undefined, args);

			if (matched) {
				var token = {
					type: matched.type,
					value: matched.value,
					loc: location(line, column, index, matched.line, matched.column, matched.index, settings.source)
				};

				tokens.push(token);
				index = matched.index;
				line = matched.line;
				column = matched.column;
			} else {
				error(tokenizeErrorTypes.cannotTokenizeSymbol(input.charAt(index), line, column), input, line, column);
			}
		}

		return tokens;
	}

	var literals = [tokenTypes.STRING, tokenTypes.NUMBER, tokenTypes.TRUE, tokenTypes.FALSE, tokenTypes.NULL];

	var objectStates = {
		_START_: 0,
		OPEN_OBJECT: 1,
		PROPERTY: 2,
		COMMA: 3
	};

	var propertyStates = {
		_START_: 0,
		KEY: 1,
		COLON: 2
	};

	var arrayStates = {
		_START_: 0,
		OPEN_ARRAY: 1,
		VALUE: 2,
		COMMA: 3
	};

	var defaultSettings = {
		verbose: true,
		source: null
	};

	function parseObject(input, tokenList, index, settings) {
		// object: LEFT_BRACE (property (COMMA property)*)? RIGHT_BRACE
		var startToken = void 0;
		var object = {
			type: 'object',
			children: []
		};
		var state = objectStates._START_;

		while (index < tokenList.length) {
			var token = tokenList[index];

			switch (state) {
				case objectStates._START_:
					{
						if (token.type === tokenTypes.LEFT_BRACE) {
							startToken = token;
							state = objectStates.OPEN_OBJECT;
							index++;
						} else {
							return null;
						}
						break;
					}

				case objectStates.OPEN_OBJECT:
					{
						if (token.type === tokenTypes.RIGHT_BRACE) {
							if (settings.verbose) {
								object.loc = location(startToken.loc.start.line, startToken.loc.start.column, startToken.loc.start.offset, token.loc.end.line, token.loc.end.column, token.loc.end.offset, settings.source);
								return {
									value: object,
									index: index + 1
								};
							}
						} else {
							var property = parseProperty(input, tokenList, index, settings);
							object.children.push(property.value);
							state = objectStates.PROPERTY;
							index = property.index;
						}
						break;
					}

				case objectStates.PROPERTY:
					{
						if (token.type === tokenTypes.RIGHT_BRACE) {
							if (settings.verbose) {
								object.loc = location(startToken.loc.start.line, startToken.loc.start.column, startToken.loc.start.offset, token.loc.end.line, token.loc.end.column, token.loc.end.offset, settings.source);
							}
							return {
								value: object,
								index: index + 1
							};
						} else if (token.type === tokenTypes.COMMA) {
							state = objectStates.COMMA;
							index++;
						} else {
							error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
						}
						break;
					}

				case objectStates.COMMA:
					{
						var _property = parseProperty(input, tokenList, index, settings);
						if (_property) {
							index = _property.index;
							object.children.push(_property.value);
							state = objectStates.PROPERTY;
						} else {
							error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
						}
						break;
					}
			}
		}

		error(parseErrorTypes.unexpectedEnd());
	}

	function parseProperty(input, tokenList, index, settings) {
		// property: STRING COLON value
		var startToken = void 0;
		var property = {
			type: 'property',
			key: null,
			value: null
		};
		var state = objectStates._START_;

		while (index < tokenList.length) {
			var token = tokenList[index];

			switch (state) {
				case propertyStates._START_:
					{
						if (token.type === tokenTypes.STRING) {
							var key = {
								type: 'identifier',
								value: token.value
							};
							if (settings.verbose) {
								key.loc = token.loc;
							}
							startToken = token;
							property.key = key;
							state = propertyStates.KEY;
							index++;
						} else {
							return null;
						}
						break;
					}

				case propertyStates.KEY:
					{
						if (token.type === tokenTypes.COLON) {
							state = propertyStates.COLON;
							index++;
						} else {
							error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
						}
						break;
					}

				case propertyStates.COLON:
					{
						var value = parseValue(input, tokenList, index, settings);
						property.value = value.value;
						if (settings.verbose) {
							property.loc = location(startToken.loc.start.line, startToken.loc.start.column, startToken.loc.start.offset, value.value.loc.end.line, value.value.loc.end.column, value.value.loc.end.offset, settings.source);
						}
						return {
							value: property,
							index: value.index
						};
					}

			}
		}
	}

	function parseArray(input, tokenList, index, settings) {
		// array: LEFT_BRACKET (value (COMMA value)*)? RIGHT_BRACKET
		var startToken = void 0;
		var array = {
			type: 'array',
			children: []
		};
		var state = arrayStates._START_;
		var token = void 0;

		while (index < tokenList.length) {
			token = tokenList[index];

			switch (state) {
				case arrayStates._START_:
					{
						if (token.type === tokenTypes.LEFT_BRACKET) {
							startToken = token;
							state = arrayStates.OPEN_ARRAY;
							index++;
						} else {
							return null;
						}
						break;
					}

				case arrayStates.OPEN_ARRAY:
					{
						if (token.type === tokenTypes.RIGHT_BRACKET) {
							if (settings.verbose) {
								array.loc = location(startToken.loc.start.line, startToken.loc.start.column, startToken.loc.start.offset, token.loc.end.line, token.loc.end.column, token.loc.end.offset, settings.source);
							}
							return {
								value: array,
								index: index + 1
							};
						} else {
							var value = parseValue(input, tokenList, index, settings);
							index = value.index;
							array.children.push(value.value);
							state = arrayStates.VALUE;
						}
						break;
					}

				case arrayStates.VALUE:
					{
						if (token.type === tokenTypes.RIGHT_BRACKET) {
							if (settings.verbose) {
								array.loc = location(startToken.loc.start.line, startToken.loc.start.column, startToken.loc.start.offset, token.loc.end.line, token.loc.end.column, token.loc.end.offset, settings.source);
							}
							index++;
							return {
								value: array,
								index: index
							};
						} else if (token.type === tokenTypes.COMMA) {
							state = arrayStates.COMMA;
							index++;
						} else {
							error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
						}
						break;
					}

				case arrayStates.COMMA:
					{
						var _value = parseValue(input, tokenList, index, settings);
						index = _value.index;
						array.children.push(_value.value);
						state = arrayStates.VALUE;
						break;
					}
			}
		}

		error(parseErrorTypes.unexpectedEnd());
	}

	function parseLiteral(input, tokenList, index, settings) {
		// literal: STRING | NUMBER | TRUE | FALSE | NULL
		var token = tokenList[index];

		var isLiteral = literals.indexOf(token.type) !== -1;

		if (isLiteral) {
			var literal = {
				type: 'literal',
				value: token.value,
				rawValue: input.substring(token.loc.start.offset, token.loc.end.offset)
			};
			if (settings.verbose) {
				literal.loc = token.loc;
			}
			return {
				value: literal,
				index: index + 1
			};
		}

		return null;
	}

	function parseValue(input, tokenList, index, settings) {
		// value: literal | object | array
		var token = tokenList[index];

		var value = parseLiteral.apply(undefined, arguments) || parseObject.apply(undefined, arguments) || parseArray.apply(undefined, arguments);

		if (value) {
			return value;
		} else {
			error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
		}
	}

	var parse = function parse(input, settings) {
		settings = _extends({}, defaultSettings, settings);
		var tokenList = tokenize(input, settings);

		if (tokenList.length === 0) {
			error(parseErrorTypes.unexpectedEnd());
		}

		var value = parseValue(input, tokenList, 0, settings);

		if (value.index === tokenList.length) {
			return value.value;
		} else {
			var token = tokenList[value.index];
			error(parseErrorTypes.unexpectedToken(input.substring(token.loc.start.offset, token.loc.end.offset), token.loc.start.line, token.loc.start.column), input, token.loc.start.line, token.loc.start.column);
		}
	};

	module.exports = parse;
});
},{}],34:[function(require,module,exports){
/*
 * Copyright 2014-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

var rest = require('./client/default'),
    browser = require('./client/xhr');

rest.setPlatformDefaultClient(browser);

module.exports = rest;

},{"./client/default":36,"./client/xhr":37}],35:[function(require,module,exports){
/*
 * Copyright 2014-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

/**
 * Add common helper methods to a client impl
 *
 * @param {function} impl the client implementation
 * @param {Client} [target] target of this client, used when wrapping other clients
 * @returns {Client} the client impl with additional methods
 */
module.exports = function client(impl, target) {

	if (target) {

		/**
		 * @returns {Client} the target client
		 */
		impl.skip = function skip() {
			return target;
		};

	}

	/**
	 * Allow a client to easily be wrapped by an interceptor
	 *
	 * @param {Interceptor} interceptor the interceptor to wrap this client with
	 * @param [config] configuration for the interceptor
	 * @returns {Client} the newly wrapped client
	 */
	impl.wrap = function wrap(interceptor, config) {
		return interceptor(impl, config);
	};

	/**
	 * @deprecated
	 */
	impl.chain = function chain() {
		if (typeof console !== 'undefined') {
			console.log('rest.js: client.chain() is deprecated, use client.wrap() instead');
		}

		return impl.wrap.apply(this, arguments);
	};

	return impl;

};

},{}],36:[function(require,module,exports){
/*
 * Copyright 2014-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

/**
 * Plain JS Object containing properties that represent an HTTP request.
 *
 * Depending on the capabilities of the underlying client, a request
 * may be cancelable. If a request may be canceled, the client will add
 * a canceled flag and cancel function to the request object. Canceling
 * the request will put the response into an error state.
 *
 * @field {string} [method='GET'] HTTP method, commonly GET, POST, PUT, DELETE or HEAD
 * @field {string|UrlBuilder} [path=''] path template with optional path variables
 * @field {Object} [params] parameters for the path template and query string
 * @field {Object} [headers] custom HTTP headers to send, in addition to the clients default headers
 * @field [entity] the HTTP entity, common for POST or PUT requests
 * @field {boolean} [canceled] true if the request has been canceled, set by the client
 * @field {Function} [cancel] cancels the request if invoked, provided by the client
 * @field {Client} [originator] the client that first handled this request, provided by the interceptor
 *
 * @class Request
 */

/**
 * Plain JS Object containing properties that represent an HTTP response
 *
 * @field {Object} [request] the request object as received by the root client
 * @field {Object} [raw] the underlying request object, like XmlHttpRequest in a browser
 * @field {number} [status.code] status code of the response (i.e. 200, 404)
 * @field {string} [status.text] status phrase of the response
 * @field {Object] [headers] response headers hash of normalized name, value pairs
 * @field [entity] the response body
 *
 * @class Response
 */

/**
 * HTTP client particularly suited for RESTful operations.
 *
 * @field {function} wrap wraps this client with a new interceptor returning the wrapped client
 *
 * @param {Request} the HTTP request
 * @returns {ResponsePromise<Response>} a promise the resolves to the HTTP response
 *
 * @class Client
 */

 /**
  * Extended when.js Promises/A+ promise with HTTP specific helpers
  *q
  * @method entity promise for the HTTP entity
  * @method status promise for the HTTP status code
  * @method headers promise for the HTTP response headers
  * @method header promise for a specific HTTP response header
  *
  * @class ResponsePromise
  * @extends Promise
  */

var client, target, platformDefault;

client = require('../client');

if (typeof Promise !== 'function' && console && console.log) {
	console.log('An ES6 Promise implementation is required to use rest.js. See https://github.com/cujojs/when/blob/master/docs/es6-promise-shim.md for using when.js as a Promise polyfill.');
}

/**
 * Make a request with the default client
 * @param {Request} the HTTP request
 * @returns {Promise<Response>} a promise the resolves to the HTTP response
 */
function defaultClient() {
	return target.apply(void 0, arguments);
}

/**
 * Change the default client
 * @param {Client} client the new default client
 */
defaultClient.setDefaultClient = function setDefaultClient(client) {
	target = client;
};

/**
 * Obtain a direct reference to the current default client
 * @returns {Client} the default client
 */
defaultClient.getDefaultClient = function getDefaultClient() {
	return target;
};

/**
 * Reset the default client to the platform default
 */
defaultClient.resetDefaultClient = function resetDefaultClient() {
	target = platformDefault;
};

/**
 * @private
 */
defaultClient.setPlatformDefaultClient = function setPlatformDefaultClient(client) {
	if (platformDefault) {
		throw new Error('Unable to redefine platformDefaultClient');
	}
	target = platformDefault = client;
};

module.exports = client(defaultClient);

},{"../client":35}],37:[function(require,module,exports){
/*
 * Copyright 2012-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

var normalizeHeaderName, responsePromise, client, headerSplitRE;

normalizeHeaderName = require('../util/normalizeHeaderName');
responsePromise = require('../util/responsePromise');
client = require('../client');

// according to the spec, the line break is '\r\n', but doesn't hold true in practice
headerSplitRE = /[\r|\n]+/;

function parseHeaders(raw) {
	// Note: Set-Cookie will be removed by the browser
	var headers = {};

	if (!raw) { return headers; }

	raw.trim().split(headerSplitRE).forEach(function (header) {
		var boundary, name, value;
		boundary = header.indexOf(':');
		name = normalizeHeaderName(header.substring(0, boundary).trim());
		value = header.substring(boundary + 1).trim();
		if (headers[name]) {
			if (Array.isArray(headers[name])) {
				// add to an existing array
				headers[name].push(value);
			}
			else {
				// convert single value to array
				headers[name] = [headers[name], value];
			}
		}
		else {
			// new, single value
			headers[name] = value;
		}
	});

	return headers;
}

function safeMixin(target, source) {
	Object.keys(source || {}).forEach(function (prop) {
		// make sure the property already exists as
		// IE 6 will blow up if we add a new prop
		if (source.hasOwnProperty(prop) && prop in target) {
			try {
				target[prop] = source[prop];
			}
			catch (e) {
				// ignore, expected for some properties at some points in the request lifecycle
			}
		}
	});

	return target;
}

module.exports = client(function xhr(request) {
	return responsePromise.promise(function (resolve, reject) {
		/*jshint maxcomplexity:20 */

		var client, method, url, headers, entity, headerName, response, XHR;

		request = typeof request === 'string' ? { path: request } : request || {};
		response = { request: request };

		if (request.canceled) {
			response.error = 'precanceled';
			reject(response);
			return;
		}

		XHR = request.engine || XMLHttpRequest;
		if (!XHR) {
			reject({ request: request, error: 'xhr-not-available' });
			return;
		}

		entity = request.entity;
		request.method = request.method || (entity ? 'POST' : 'GET');
		method = request.method;
		url = response.url = request.path || '';

		try {
			client = response.raw = new XHR();

			// mixin extra request properties before and after opening the request as some properties require being set at different phases of the request
			safeMixin(client, request.mixin);
			client.open(method, url, true);
			safeMixin(client, request.mixin);

			headers = request.headers;
			for (headerName in headers) {
				/*jshint forin:false */
				if (headerName === 'Content-Type' && headers[headerName] === 'multipart/form-data') {
					// XMLHttpRequest generates its own Content-Type header with the
					// appropriate multipart boundary when sending multipart/form-data.
					continue;
				}

				client.setRequestHeader(headerName, headers[headerName]);
			}

			request.canceled = false;
			request.cancel = function cancel() {
				request.canceled = true;
				client.abort();
				reject(response);
			};

			client.onreadystatechange = function (/* e */) {
				if (request.canceled) { return; }
				if (client.readyState === (XHR.DONE || 4)) {
					response.status = {
						code: client.status,
						text: client.statusText
					};
					response.headers = parseHeaders(client.getAllResponseHeaders());
					response.entity = client.responseText;

					// #125 -- Sometimes IE8-9 uses 1223 instead of 204
					// http://stackoverflow.com/questions/10046972/msie-returns-status-code-of-1223-for-ajax-request
					if (response.status.code === 1223) {
						response.status.code = 204;
					}

					if (response.status.code > 0) {
						// check status code as readystatechange fires before error event
						resolve(response);
					}
					else {
						// give the error callback a chance to fire before resolving
						// requests for file:// URLs do not have a status code
						setTimeout(function () {
							resolve(response);
						}, 0);
					}
				}
			};

			try {
				client.onerror = function (/* e */) {
					response.error = 'loaderror';
					reject(response);
				};
			}
			catch (e) {
				// IE 6 will not support error handling
			}

			client.send(entity);
		}
		catch (e) {
			response.error = 'loaderror';
			reject(response);
		}

	});
});

},{"../client":35,"../util/normalizeHeaderName":38,"../util/responsePromise":39}],38:[function(require,module,exports){
/*
 * Copyright 2012-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

/**
 * Normalize HTTP header names using the pseudo camel case.
 *
 * For example:
 *   content-type         -> Content-Type
 *   accepts              -> Accepts
 *   x-custom-header-name -> X-Custom-Header-Name
 *
 * @param {string} name the raw header name
 * @return {string} the normalized header name
 */
function normalizeHeaderName(name) {
	return name.toLowerCase()
		.split('-')
		.map(function (chunk) { return chunk.charAt(0).toUpperCase() + chunk.slice(1); })
		.join('-');
}

module.exports = normalizeHeaderName;

},{}],39:[function(require,module,exports){
/*
 * Copyright 2014-2016 the original author or authors
 * @license MIT, see LICENSE.txt for details
 *
 * @author Scott Andrews
 */

'use strict';

/*jshint latedef: nofunc */

var normalizeHeaderName = require('./normalizeHeaderName');

function property(promise, name) {
	return promise.then(
		function (value) {
			return value && value[name];
		},
		function (value) {
			return Promise.reject(value && value[name]);
		}
	);
}

/**
 * Obtain the response entity
 *
 * @returns {Promise} for the response entity
 */
function entity() {
	/*jshint validthis:true */
	return property(this, 'entity');
}

/**
 * Obtain the response status
 *
 * @returns {Promise} for the response status
 */
function status() {
	/*jshint validthis:true */
	return property(property(this, 'status'), 'code');
}

/**
 * Obtain the response headers map
 *
 * @returns {Promise} for the response headers map
 */
function headers() {
	/*jshint validthis:true */
	return property(this, 'headers');
}

/**
 * Obtain a specific response header
 *
 * @param {String} headerName the header to retrieve
 * @returns {Promise} for the response header's value
 */
function header(headerName) {
	/*jshint validthis:true */
	headerName = normalizeHeaderName(headerName);
	return property(this.headers(), headerName);
}

/**
 * Follow a related resource
 *
 * The relationship to follow may be define as a plain string, an object
 * with the rel and params, or an array containing one or more entries
 * with the previous forms.
 *
 * Examples:
 *   response.follow('next')
 *
 *   response.follow({ rel: 'next', params: { pageSize: 100 } })
 *
 *   response.follow([
 *       { rel: 'items', params: { projection: 'noImages' } },
 *       'search',
 *       { rel: 'findByGalleryIsNull', params: { projection: 'noImages' } },
 *       'items'
 *   ])
 *
 * @param {String|Object|Array} rels one, or more, relationships to follow
 * @returns ResponsePromise<Response> related resource
 */
function follow(rels) {
	/*jshint validthis:true */
	rels = [].concat(rels);

	return make(rels.reduce(function (response, rel) {
		return response.then(function (response) {
			if (typeof rel === 'string') {
				rel = { rel: rel };
			}
			if (typeof response.entity.clientFor !== 'function') {
				throw new Error('Hypermedia response expected');
			}
			var client = response.entity.clientFor(rel.rel);
			return client({ params: rel.params });
		});
	}, this));
}

/**
 * Wrap a Promise as an ResponsePromise
 *
 * @param {Promise<Response>} promise the promise for an HTTP Response
 * @returns {ResponsePromise<Response>} wrapped promise for Response with additional helper methods
 */
function make(promise) {
	promise.status = status;
	promise.headers = headers;
	promise.header = header;
	promise.entity = entity;
	promise.follow = follow;
	return promise;
}

function responsePromise(obj, callback, errback) {
	return make(Promise.resolve(obj).then(callback, errback));
}

responsePromise.make = make;
responsePromise.reject = function (val) {
	return make(Promise.reject(val));
};
responsePromise.promise = function (func) {
	return make(new Promise(func));
};

module.exports = responsePromise;

},{"./normalizeHeaderName":38}],40:[function(require,module,exports){
(function (process){
// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

// resolves . and .. elements in a path array with directory names there
// must be no slashes, empty elements, or device names (c:\) in the array
// (so also no leading and trailing slashes - it does not distinguish
// relative and absolute paths)
function normalizeArray(parts, allowAboveRoot) {
  // if the path tries to go above the root, `up` ends up > 0
  var up = 0;
  for (var i = parts.length - 1; i >= 0; i--) {
    var last = parts[i];
    if (last === '.') {
      parts.splice(i, 1);
    } else if (last === '..') {
      parts.splice(i, 1);
      up++;
    } else if (up) {
      parts.splice(i, 1);
      up--;
    }
  }

  // if the path is allowed to go above the root, restore leading ..s
  if (allowAboveRoot) {
    for (; up--; up) {
      parts.unshift('..');
    }
  }

  return parts;
}

// Split a filename into [root, dir, basename, ext], unix version
// 'root' is just a slash, or nothing.
var splitPathRe =
    /^(\/?|)([\s\S]*?)((?:\.{1,2}|[^\/]+?|)(\.[^.\/]*|))(?:[\/]*)$/;
var splitPath = function(filename) {
  return splitPathRe.exec(filename).slice(1);
};

// path.resolve([from ...], to)
// posix version
exports.resolve = function() {
  var resolvedPath = '',
      resolvedAbsolute = false;

  for (var i = arguments.length - 1; i >= -1 && !resolvedAbsolute; i--) {
    var path = (i >= 0) ? arguments[i] : process.cwd();

    // Skip empty and invalid entries
    if (typeof path !== 'string') {
      throw new TypeError('Arguments to path.resolve must be strings');
    } else if (!path) {
      continue;
    }

    resolvedPath = path + '/' + resolvedPath;
    resolvedAbsolute = path.charAt(0) === '/';
  }

  // At this point the path should be resolved to a full absolute path, but
  // handle relative paths to be safe (might happen when process.cwd() fails)

  // Normalize the path
  resolvedPath = normalizeArray(filter(resolvedPath.split('/'), function(p) {
    return !!p;
  }), !resolvedAbsolute).join('/');

  return ((resolvedAbsolute ? '/' : '') + resolvedPath) || '.';
};

// path.normalize(path)
// posix version
exports.normalize = function(path) {
  var isAbsolute = exports.isAbsolute(path),
      trailingSlash = substr(path, -1) === '/';

  // Normalize the path
  path = normalizeArray(filter(path.split('/'), function(p) {
    return !!p;
  }), !isAbsolute).join('/');

  if (!path && !isAbsolute) {
    path = '.';
  }
  if (path && trailingSlash) {
    path += '/';
  }

  return (isAbsolute ? '/' : '') + path;
};

// posix version
exports.isAbsolute = function(path) {
  return path.charAt(0) === '/';
};

// posix version
exports.join = function() {
  var paths = Array.prototype.slice.call(arguments, 0);
  return exports.normalize(filter(paths, function(p, index) {
    if (typeof p !== 'string') {
      throw new TypeError('Arguments to path.join must be strings');
    }
    return p;
  }).join('/'));
};


// path.relative(from, to)
// posix version
exports.relative = function(from, to) {
  from = exports.resolve(from).substr(1);
  to = exports.resolve(to).substr(1);

  function trim(arr) {
    var start = 0;
    for (; start < arr.length; start++) {
      if (arr[start] !== '') break;
    }

    var end = arr.length - 1;
    for (; end >= 0; end--) {
      if (arr[end] !== '') break;
    }

    if (start > end) return [];
    return arr.slice(start, end - start + 1);
  }

  var fromParts = trim(from.split('/'));
  var toParts = trim(to.split('/'));

  var length = Math.min(fromParts.length, toParts.length);
  var samePartsLength = length;
  for (var i = 0; i < length; i++) {
    if (fromParts[i] !== toParts[i]) {
      samePartsLength = i;
      break;
    }
  }

  var outputParts = [];
  for (var i = samePartsLength; i < fromParts.length; i++) {
    outputParts.push('..');
  }

  outputParts = outputParts.concat(toParts.slice(samePartsLength));

  return outputParts.join('/');
};

exports.sep = '/';
exports.delimiter = ':';

exports.dirname = function(path) {
  var result = splitPath(path),
      root = result[0],
      dir = result[1];

  if (!root && !dir) {
    // No dirname whatsoever
    return '.';
  }

  if (dir) {
    // It has a dirname, strip trailing slash
    dir = dir.substr(0, dir.length - 1);
  }

  return root + dir;
};


exports.basename = function(path, ext) {
  var f = splitPath(path)[2];
  // TODO: make this comparison case-insensitive on windows?
  if (ext && f.substr(-1 * ext.length) === ext) {
    f = f.substr(0, f.length - ext.length);
  }
  return f;
};


exports.extname = function(path) {
  return splitPath(path)[3];
};

function filter (xs, f) {
    if (xs.filter) return xs.filter(f);
    var res = [];
    for (var i = 0; i < xs.length; i++) {
        if (f(xs[i], i, xs)) res.push(xs[i]);
    }
    return res;
}

// String.prototype.substr - negative index don't work in IE8
var substr = 'ab'.substr(-1) === 'b'
    ? function (str, start, len) { return str.substr(start, len) }
    : function (str, start, len) {
        if (start < 0) start = str.length + start;
        return str.substr(start, len);
    }
;

}).call(this,require('_process'))
},{"_process":41}],41:[function(require,module,exports){
// shim for using process in browser
var process = module.exports = {};

// cached from whatever global is present so that test runners that stub it
// don't break things.  But we need to wrap it in a try catch in case it is
// wrapped in strict mode code which doesn't define any globals.  It's inside a
// function because try/catches deoptimize in certain engines.

var cachedSetTimeout;
var cachedClearTimeout;

function defaultSetTimout() {
    throw new Error('setTimeout has not been defined');
}
function defaultClearTimeout () {
    throw new Error('clearTimeout has not been defined');
}
(function () {
    try {
        if (typeof setTimeout === 'function') {
            cachedSetTimeout = setTimeout;
        } else {
            cachedSetTimeout = defaultSetTimout;
        }
    } catch (e) {
        cachedSetTimeout = defaultSetTimout;
    }
    try {
        if (typeof clearTimeout === 'function') {
            cachedClearTimeout = clearTimeout;
        } else {
            cachedClearTimeout = defaultClearTimeout;
        }
    } catch (e) {
        cachedClearTimeout = defaultClearTimeout;
    }
} ())
function runTimeout(fun) {
    if (cachedSetTimeout === setTimeout) {
        //normal enviroments in sane situations
        return setTimeout(fun, 0);
    }
    // if setTimeout wasn't available but was latter defined
    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
        cachedSetTimeout = setTimeout;
        return setTimeout(fun, 0);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedSetTimeout(fun, 0);
    } catch(e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
            return cachedSetTimeout.call(null, fun, 0);
        } catch(e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
            return cachedSetTimeout.call(this, fun, 0);
        }
    }


}
function runClearTimeout(marker) {
    if (cachedClearTimeout === clearTimeout) {
        //normal enviroments in sane situations
        return clearTimeout(marker);
    }
    // if clearTimeout wasn't available but was latter defined
    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
        cachedClearTimeout = clearTimeout;
        return clearTimeout(marker);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedClearTimeout(marker);
    } catch (e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
            return cachedClearTimeout.call(null, marker);
        } catch (e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
            // Some versions of I.E. have different rules for clearTimeout vs setTimeout
            return cachedClearTimeout.call(this, marker);
        }
    }



}
var queue = [];
var draining = false;
var currentQueue;
var queueIndex = -1;

function cleanUpNextTick() {
    if (!draining || !currentQueue) {
        return;
    }
    draining = false;
    if (currentQueue.length) {
        queue = currentQueue.concat(queue);
    } else {
        queueIndex = -1;
    }
    if (queue.length) {
        drainQueue();
    }
}

function drainQueue() {
    if (draining) {
        return;
    }
    var timeout = runTimeout(cleanUpNextTick);
    draining = true;

    var len = queue.length;
    while(len) {
        currentQueue = queue;
        queue = [];
        while (++queueIndex < len) {
            if (currentQueue) {
                currentQueue[queueIndex].run();
            }
        }
        queueIndex = -1;
        len = queue.length;
    }
    currentQueue = null;
    draining = false;
    runClearTimeout(timeout);
}

process.nextTick = function (fun) {
    var args = new Array(arguments.length - 1);
    if (arguments.length > 1) {
        for (var i = 1; i < arguments.length; i++) {
            args[i - 1] = arguments[i];
        }
    }
    queue.push(new Item(fun, args));
    if (queue.length === 1 && !draining) {
        runTimeout(drainQueue);
    }
};

// v8 likes predictible objects
function Item(fun, array) {
    this.fun = fun;
    this.array = array;
}
Item.prototype.run = function () {
    this.fun.apply(null, this.array);
};
process.title = 'browser';
process.browser = true;
process.env = {};
process.argv = [];
process.version = ''; // empty string to avoid regexp issues
process.versions = {};

function noop() {}

process.on = noop;
process.addListener = noop;
process.once = noop;
process.off = noop;
process.removeListener = noop;
process.removeAllListeners = noop;
process.emit = noop;

process.binding = function (name) {
    throw new Error('process.binding is not supported');
};

process.cwd = function () { return '/' };
process.chdir = function (dir) {
    throw new Error('process.chdir is not supported');
};
process.umask = function() { return 0; };

},{}]},{},[32])(32)
});