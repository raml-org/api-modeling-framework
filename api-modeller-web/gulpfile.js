'use strict';

var gulp = require('gulp');

var browserify = require('browserify');
var tsify = require('tsify');
var watchify = require('watchify');
var babelify = require('babelify');
var source = require('vinyl-source-stream');
var buffer = require('vinyl-buffer');
var gutil = require('gulp-util');
var sourcemaps = require('gulp-sourcemaps');
var browserSync = require('browser-sync').create();

var child = require("child_process");

gulp.task("deps", function() {
    console.log("* Local NPM deps");
    child.execSync("npm install");

    console.log("* Building Clojurescript dependency");
    console.log(child.execSync("rm -f ../index_package.js").toString());
    console.log(child.execSync("cd .. && lein cljsbuild once web").toString());
    console.log(child.execSync("mkdir -p public/js").toString());
    console.log(child.execSync("cp -rf ../target/cljsbuild-compiler-1 public/js/").toString());
    console.log(child.execSync("cp -rf ../index_package.js public/js/amf.js").toString());
});

const options = {"standalone":"api_modeller"};
const b = watchify(browserify(options));
function bundle() {
    return b
        .add('src/view_model.ts')
        .plugin(tsify, { target: 'es6' })
        .transform(babelify, { extensions: [ '.tsx', '.ts' ] })
        .bundle()
        // log errors if they happen
        .on('error', gutil.log.bind(gutil, 'Browserify Error'))
        .pipe(source('api_modeller.js'))
        // optional, remove if you don't need to buffer file contents
        .pipe(buffer())
        // optional, remove if you dont want sourcemaps
        .pipe(sourcemaps.init({loadMaps: true})) // loads map from browserify file
        // Add transformation tasks to the pipeline here.
        .pipe(sourcemaps.write('./')) // writes .map file
        .pipe(gulp.dest('./public/js'))
        .pipe(browserSync.stream({once: true}));
}
gulp.task('bundle', ['deps'], bundle); // so you can run `gulp js` to build the file
b.on('update', bundle); // on any dep update, runs the bundler
b.on('log', gutil.log); // output build logs to terminal


gulp.task('serve', ['bundle'], function () {
    browserSync.init({
        server: "public",
        startPath: "/index.html"
    });
});

gulp.task('force', function() {
   bundle();
    browserSync.init({
        server: "public",
        startPath: "/index.html"
    });
});