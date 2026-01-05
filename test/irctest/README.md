# irctest Suite

[irctest](https://github.com/progval/irctest) is a suite of server-side tests for validating IRC protocol compatibility. While it does not guarantee full compliance, it is a valuable tool for ensuring a server meets a reasonable baseline of IRC behavior.

## Running the Tests

1. Clone the irctest repository from <https://github.com/progval/irctest> and follow its setup instructions.
2. Copy `ritsirc.py` into the `irctest/irctest/controllers` directory.
3. Verify that the path to the JAR file hard-coded in `ritsirc.py` is correct for your environment.
4. Run the test suite with the following command:

```bash
pytest --controller irctest.controllers.ritsirc -k 'not Ergo and not deprecated and not strict and not draft and not extended-monitor and not batch and not message_tags.py and not multi_prefix.py'
```

## Test Exclusions

The following test groups are intentionally excluded:

- **Ergo**  
  irctest is the integration test suite for Ergo and includes application-specific scenarios that are not applicable to most IRC servers.

- **deprecated**  
  Supporting deprecated features is not a goal of this project.

- **strict**  
  irctest recommends excluding these tests in most cases.

- **draft**  
  Supporting draft features is not a goal of this project.

- **extended-monitor**  
  `ritsirc` does not support this IRCv3 capability.

- **batch**  
  `ritsirc` does not support this IRCv3 capability.

- **message_tags**  
  `ritsirc` does not support this IRCv3 capability.

- **multi_prefix**  
  `ritsirc` does not support this IRCv3 capability.
