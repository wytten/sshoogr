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

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.junit.Test

import static com.aestasit.infrastructure.ssh.DefaultSsh.*
import static org.junit.Assert.fail

/**
 * Test for static default SSH DSL implementation.
 *
 * @author Andrey Adamovich
 *
 */
class DefaultSshTest extends BaseSshTest {

  @Test
  void testStaticMethods() {
    trustUnknownHosts = true
    execOptions {
      maxWait = 30000
    }
    String output = captureOutput {
      remoteSession('user2:654321@localhost:2233') {
        exec 'whoami'
        exec 'du -s'
        exec 'rm -rf /tmp/test.file'
        scp testFile, '/tmp/test.file'
        remoteFile('/etc/init.conf').text = 'content'
      }
    }
    assert output.contains('whoami')
    assert output.contains('test.file')
    assert output.contains('100%')
  }

  @Test
  void testUnknownHosts() {
    trustUnknownHosts = false
    knownHostsFile = new File('build/no-such-known_hosts')
    try {
      remoteSession('user2:654321@localhost:2233') {
        exec 'whoami'
      }
      fail('Should fail with host reject!')
    } catch (JSchException e) {
      assert e.message.contains('reject')
    } finally {
      knownHostsFile = null
    }
  }

  @Test
  void testKnownHostsFileAcceptsKnownHost() {
    File knownHosts = writeKnownHosts(serverHostKeyLine())
    trustUnknownHosts = false
    knownHostsFile = knownHosts
    try {
      String output = captureOutput {
        remoteSession('user2:654321@localhost:2233') {
          exec 'whoami'
        }
      }
      assert output.contains('root')
    } finally {
      knownHostsFile = null
      knownHosts.delete()
    }
  }

  @Test
  void testKnownHostsFileRejectsChangedKey() {
    // Same host and key type, but a different (valid) key from another generated key pair.
    String[] real = serverHostKeyLine().split(' ')
    String otherKey = real[1] == 'ssh-dss' ? otherDsaPublicKey() : otherRsaPublicKey()
    File knownHosts = writeKnownHosts("${real[0]} ${real[1]} ${otherKey}")
    trustUnknownHosts = false
    knownHostsFile = knownHosts
    try {
      remoteSession('user2:654321@localhost:2233') {
        exec 'whoami'
      }
      fail('Should fail because the host key changed!')
    } catch (JSchException e) {
      assert e.message.contains('HostKey has been changed')
    } finally {
      knownHostsFile = null
      knownHosts.delete()
    }
  }

  private static String serverHostKeyLine() {
    Session session = new JSch().getSession('user2', 'localhost', 2233)
    session.password = '654321'
    session.setConfig('StrictHostKeyChecking', 'no')
    session.setConfig('PreferredAuthentications', 'password')
    session.connect()
    try {
      def hk = session.hostKey
      "${hk.host} ${hk.type} ${hk.key}"
    } finally {
      session.disconnect()
    }
  }

  private static File writeKnownHosts(String line) {
    File file = File.createTempFile('known_hosts', '')
    file.text = line + '\n'
    file
  }

  private static String otherRsaPublicKey() {
    publicKeyBlob(com.jcraft.jsch.KeyPair.RSA)
  }

  private static String otherDsaPublicKey() {
    publicKeyBlob(com.jcraft.jsch.KeyPair.DSA)
  }

  private static String publicKeyBlob(int type) {
    def kp = com.jcraft.jsch.KeyPair.genKeyPair(new JSch(), type, type == com.jcraft.jsch.KeyPair.DSA ? 1024 : 2048)
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    kp.writePublicKey(out, '')
    new String(out.toByteArray()).trim().split(' ')[1]
  }

  @Test
  void testOptionsOverride() {
    trustUnknownHosts = true

    // Default behaviour should show the progress.
    String output = captureOutput {
      remoteSession('user2:654321@localhost:2233') {
        scp {
          from { localDir new File(currentDir, 'test-settings') }
          into { remoteDir '/tmp/puppet' }
        }
      }
    }
    assert output.contains('100%')

    // Test scp delegate override.
    output = captureOutput {
      remoteSession('user2:654321@localhost:2233') {
        scp {
          showProgress = false
          from { localDir new File(currentDir, 'test-settings') }
          into { remoteDir '/tmp/puppet' }
        }
      }
    }
    assert !output.contains('bytes transferred')

    // Test global override inside session closure.
    output = captureOutput {
      remoteSession('user2:654321@localhost:2233') {
        scpOptions {
          showProgress = false
        }
        scp {
          from { localDir new File(currentDir, 'test-settings') }
          into { remoteDir '/tmp/puppet' }
        }
      }
    }
    assert !output.contains('bytes transferred')

  }
}