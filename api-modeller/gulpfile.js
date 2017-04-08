'use strict';

var gulp = require('gulp');
var electron = require('electron-connect').server.create();
var typescript = require('gulp-tsc');
var bower = require('gulp-bower');

gulp.task('compile', function(){
    gulp.src(['typings/index.d.ts','src/**/*.ts'])
        .pipe(typescript({emitError: false}))
        .pipe(gulp.dest('src/'));
});

gulp.task('bower', function() {
    return bower({cwd: "public"});
});

gulp.task('serve', ['compile','bower'], function () {

    // Start browser process
    electron.start();

    // Restart browser process
    gulp.watch('src/main.js', electron.restart);

    // Reload renderer process
    gulp.watch(['src/**/*.js', 'public/**/*.html', 'public/**/*.css'], electron.reload);
});
