@Grab('com.aestasit.infrastructure.sshoogr:sshoogr:0.9.25')
import static com.aestasit.infrastructure.ssh.DefaultSsh.*

options.trustUnknownHosts=true

remoteSession {
  user = 'gigatst'
  keyFile = new File(System.getenv('HOME') +'/.ssh/id_rsa')
  host = 'lova.decare.com'
  connect()
  println "====================================================="
  exec 'ls -ltr /tmp'
  println "====================================================="
  scp {
    from { localFile('examples/firstLight.groovy') }
    into { remoteDir('/tmp') }
  }
  println "====================================================="
  exec 'ls -ltr /tmp'
}

