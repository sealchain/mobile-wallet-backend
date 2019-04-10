# mobile-wallet-backend

Seal mobile wallet backend project.

## Usage

First, put `conf/config.clj` in the working dir and change config values in this file.

Check:

$ tree .

should output like this:
.
|-mobile_wallet_backend-0.1.0-standalone.jar
|-conf
| |---config.clj

Production deployment:

$ nohup java -jar -Xmx4096m mobile_wallet_backend-0.1.0-standalone.jar > /dev/null 2>&1 &

Or run `java -jar mobile_wallet_backend-0.1.0-standalone.jar` as daemon process.

