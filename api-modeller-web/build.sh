pushd ../
lein cljsbuild once web
popd
mkdir -p ./public/js
cp ../index_package.js ./public/js/amf.js
node build.js > ./public/js/api_modeller.js
