public class ConnectionInfo {

    public ConnectionInfo() {
        if (EmsConfig.get("jms.ctxfactory") != null) {
            ctxFactory = EmsConfig.get("jms.ctxfactory");
            System.out.println(ctxFactory);
        }
        if (EmsConfig.get("jms.connfactory") != null) {
            connFactory = EmsConfig.get("jms.connfactory");
            System.out.println(connFactory);
        }
        if (EmsConfig.get("jms.username") != null) {
            username = EmsConfig.get("jms.username");
        }
        if (EmsConfig.get("jms.password") != null) {
            password = EmsConfig.get("jms.password");
        }
        if (EmsConfig.get("jms.destination") != null) {
            destination = EmsConfig.get("jms.destination");
        }
        if (EmsConfig.get("jms.uri") != null) {
            uri = EmsConfig.get("jms.uri");
        }
    }
}
}
