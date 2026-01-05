from pathlib import Path
import tempfile
from typing import Optional, TextIO, Type, cast

from irctest import authentication, tls
from irctest.basecontrollers import (
    BaseClientController,
    NotImplementedByController,
    TestCaseControllerConfig, BaseServerController,
)

class RitsIRCController(BaseServerController):
    software_name = "RitsIRC"
    supported_sasl_mechanisms = {}
    supports_sts = False

    def __init__(self, test_config: TestCaseControllerConfig):
        super().__init__(test_config)

    def run(
        self,
        hostname: str,
        port: int,
        *,
        password: Optional[str],
        ssl: bool,
        run_services: bool,
        faketime: Optional[str],
    ) -> None:
        if ssl:
            raise NotImplementedByController("SSL")
        if run_services:
            raise NotImplementedByController("Services")
        if faketime:
            raise NotImplementedByController("FakeTime")
        assert self.proc is None
        self.port = port
        self.hostname = hostname
        if password:
            self.proc = self.execute([
                "java",
                "-cp",
                "../toy-irc-server-client/target/irc-1.0.0-SNAPSHOT.jar",
                "com.jessegrabowski.irc.server.IRCServer",
                "-L",
                "FINE",
                "-H",
                hostname,
                "-p",
                str(port),
                "-P",
                password,
                "-S",
                "My.Little.Server",
                "-o",
                "operuser",
                "-O",
                "operpassword"
            ])
        else:
            self.proc = self.execute([
                "java",
                "-cp",
                "../toy-irc-server-client/target/irc-1.0.0-SNAPSHOT.jar",
                "com.jessegrabowski.irc.server.IRCServer",
                "-L",
                "FINE",
                "-H",
                hostname,
                "-p",
                str(port),
                "-S",
                "My.Little.Server",
                "-o",
                "operuser",
                "-O",
                "operpassword"
            ])

def get_irctest_controller_class() -> Type[RitsIRCController]:
    return RitsIRCController
