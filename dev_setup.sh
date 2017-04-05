#!/bin/bash

lein npm install
# lein cljsbuild once default
lein cljsbuild once web
ln -s $(pwd)/js $(pwd)/node/js
cd api-modeller
tsc
npm install
cd ..
ln -s $(pwd)/node/engine $(pwd)/api-modeller/node_modules/api_modelling_framework
cp package_files/* api-modeller/node_modules/api_modelling_framework/
pushd api-modeller/public
bower install
popd
cd api-modeller && gulp serve
