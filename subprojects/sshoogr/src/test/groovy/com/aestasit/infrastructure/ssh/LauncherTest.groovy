/*
 * Copyright (C) 2011-2020 Aestas/IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aestasit.infrastructure.ssh

import com.aestasit.infrastructure.ssh.launcher.Sshoogr
import org.junit.Test

class LauncherTest extends BaseSshTest {

  protected static final String SCRIPT = '''
    remoteSession {
      exec 'whoami'
      exec 'du -s'
      exec 'rm -rf /tmp/test.file'
      remoteFile('/etc/init.conf').text = 'content'
    }
    '''

  @Test
  void scriptWithConnectionParameters() {
    int status = -1
    String output = captureOutput {
      status = Sshoogr.run([

        '--user',
        'user2',

        '--password',
        '654321',

        '--host',
        'localhost',

        '--trust',

        '--port',
        '2233',

        temporaryScript.absolutePath

      ] as String[])
    }
    assert status == 0
    assert output.contains('Connecting')
  }

  @Test
  void helpMessage() {
    int status = -1
    String output = captureOutput {
      status = Sshoogr.run(['--help'] as String[])
    }
    assert status == 0
    assert output.contains('Usage:')
  }

  @Test
  void notExistingScript() {
    assert Sshoogr.run(['gg.sshoogr'] as String[]) == 127
  }

  static File getTemporaryScript() {
    File scriptFile = File.createTempFile('default', '.sshoogr')
    scriptFile.text = SCRIPT
    scriptFile
  }

}
