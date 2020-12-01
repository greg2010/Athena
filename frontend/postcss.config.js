module.exports = (ctx) => {
  return {
    plugins: [
      require('postcss-import')({}),
      require('tailwindcss')({
        theme: {
          extend: {
            fontFamily: {
              condensed: [
                'Roboto Condensed'
              ]
            },
          }
        },
        purge: {
          enabled: ctx.webpackLoaderContext.mode === 'production',
          content: [
            './target/scala-2.13/*-opt.js',
            './src/main/resources/**/*.html',
          ]
        },
      }),
      require('postcss-nested')({}),
      require('autoprefixer')({}),
      ctx.webpackLoaderContext.mode === 'production' ?
        require('cssnano')() :
        false
    ]
  };
}
