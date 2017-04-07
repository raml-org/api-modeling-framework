var browserify = require('browserify');
var tsify = require('tsify');
var babelify = require('babelify');

browserify()
    .add('src/view_model.ts')
    .plugin(tsify, { target: 'es6' })
    .transform(babelify, { extensions: [ '.tsx', '.ts' ] })
    .bundle()
    .on('error', function (error) { console.error(error.toString()); })
    .pipe(process.stdout);
