const path = require('path');
const _ = require('lodash');

const ExtractCssChunks = require('extract-css-chunks-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin')

const scalaOutputPath = path.resolve(__dirname, './target/scala-2.13');
const scalaResourcesPath = path.resolve(__dirname, './src/main/resources')

const devServerPort = 8080;

const devServer = {
  hot: false,
  inline: false,
  compress: true,
  disableHostCheck: true,
  clientLogLevel: 'none',
  host: process.env.HOST || 'localhost',
  public: process.env.HOST || 'localhost',
  port: process.env.PORT || devServerPort,
  historyApiFallback: {
    index: ''
  },
  stats: {warnings: false}
};

function common(variables, mode) {
  return {
    mode: mode,
    resolve: {
      modules: [
        "node_modules",
        path.resolve(__dirname, "node_modules")
      ],
    },
    output: {
      publicPath: '/',
      filename: '[name].[hash].js',
      library: 'app',
      libraryTarget: 'var'
    },
    entry: [
      path.resolve(scalaResourcesPath, './index.css')
    ],
    module: {
      rules: [{
          test: /\.js$/,
          use: [{
            loader: "scalajs-friendly-source-map-loader",
            options: {
              name: '[name].[contenthash:8].[ext]',
              skipFileURLWarnings: true, // or false, default is true
              bundleHttp: true, // or false, default is true,
              cachePath: ".scala-js-sources", // cache dir name, exclude in .gitignore
              noisyCache: false, // whether http cache changes are output
              useCache: true, // false => remove any http cache processing
            }
          }],
          enforce: "pre",
          include: [scalaOutputPath],
        },
        {
          test: /\.js$/,
          use: ["source-map-loader"],
          enforce: "pre",
          // does not handle scala.js issued https: remote resources
          exclude: [/node_modules/, scalaOutputPath],
        },
        {
          test: /\.css$/,
          use: [{
              loader: ExtractCssChunks.loader,
              options: {
                filename: '[name].[contenthash:8].[ext]'
              }
            },
            {
              loader: 'css-loader'
            },
            {
              loader: "postcss-loader"
            }
          ]
        },
        {
          test: /\.scss$/,
          use: [{
              loader: ExtractCssChunks.loader,
              options: {
                filename: '[name].[contenthash:8].[ext]'
              }
            },
            {
              loader: 'css-loader'
            },
            {
              loader: "postcss-loader",
              options: {
                config: {
                  path: path.resolve(__dirname, './postcss.config.js')
                }
              }
            },
            {
              loader: 'sass-loader'
            }
          ]
        },
        {
          test: /\.(woff(2)?|ttf|eot|svg|png|jpg|ico|txt|json)(\?v=\d+\.\d+\.\d+)?$/,
          use: [{
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
              outputPath: './'
            }
          }]
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        filename: 'index.html',
        template: path.resolve(scalaResourcesPath, './index.html'),
        minify: 'auto',
        inject: 'head',
        config: variables
      }),

      new ExtractCssChunks({
        filename: '[name].[hash].css',
        chunkFilename: '[id].css'
      }),
      new CopyWebpackPlugin({
        patterns: [
          {
            from: path.resolve(scalaResourcesPath, './public'),
            to: '.'
          }
        ]
      })
    ]
  }
}

const dev = {
  mode: 'development',
  entry: [
    path.resolve(__dirname, `${scalaOutputPath}/frontend-fastopt.js`)
  ],
  devtool: "eval-cheap-module-source-map",
};

const prod = {
  mode: 'production',
  entry: [
    path.resolve(__dirname, `${scalaOutputPath}/frontend-opt.js`),
  ],
  devtool: 'source-map',
  optimization: {
    minimize: true,
    minimizer: [new TerserPlugin()],
  },
  plugins: [
    new CleanWebpackPlugin()
  ],
  output: {
    path: path.resolve(__dirname, 'build')
  }
};


function customizer(objValue, srcValue) {
  if (_.isArray(objValue)) {
    return objValue.concat(srcValue);
  }
}

module.exports = function (env) {
  switch (process.env.npm_lifecycle_event) {
    case 'build':
    case 'build:prod':
      console.log('production build');
      return _.mergeWith({}, common({}, 'production'), prod, customizer);
    case 'build:dev':
      console.log('development build');
      return _.mergeWith({}, common({}, 'development'), dev, customizer);
    case 'start':
    case 'start:dev':
    default:
      console.log('development dev server');
      return _.mergeWith({}, common({}, 'development'), dev, {
        devServer
      }, customizer);
  }
}