config.set({
    processKillTimeout: 3600000,
    browserDisconnectTimeout: 3600000,
    browserNoActivityTimeout: 3600000,
    client: {
        timeout: 3600000,
        mocha: {
            timeout: 3600000
        }
    }
});