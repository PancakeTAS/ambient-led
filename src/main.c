#include "log.h"
#include "configuration.h"

int main() {
    log_info("MAIN", "Starting ambient led");

    // load configuration
    configuration_data* config = configuration_parse();
    if (!config) {
        log_error("MAIN", "Couldn't load configuration");

        return 1;
    }
    log_info("MAIN", "Configuration loaded successfully");

    // free configuration
    configuration_free(config);

    return 0;
}
