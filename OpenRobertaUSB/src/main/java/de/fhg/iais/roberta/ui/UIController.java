package de.fhg.iais.roberta.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

import de.fhg.iais.roberta.connection.Connector;
import de.fhg.iais.roberta.usb.Main;

public class UIController<ObservableObject> implements Observer {

    private final Connector connector;
    private final ConnectionView conView;
    private boolean connected;
    private final ResourceBundle rb;
    private static Logger log = Logger.getLogger("Connector");

    public UIController(Connector usbCon, ConnectionView conView, ResourceBundle rb) {
        this.connector = usbCon;
        this.conView = conView;
        this.rb = rb;
        this.connected = false;
        addListener();
    }

    public void control() {
        this.conView.setVisible(true);
    }

    private void addListener() {
        this.conView.setConnectActionListener(new ConnectActionListener());
        this.conView.setCloseListener(new CloseListener());
        ((Observable) this.connector).addObserver(this);
    }

    public class ConnectActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            if ( b.getActionCommand().equals("close") ) {
                log.info("User close");
                closeApplication();
            } else if ( b.getActionCommand().equals("about") ) {
                log.info("User about");
                showAboutPopup();
            } else if ( b.getActionCommand().equals("customaddress") ) {
                log.info("User custom address");
                showAdvancedOptions();
            } else {
                if ( b.isSelected() ) {
                    log.info("User connect");
                    checkForValidCustomServerAddressAndUpdate();
                    UIController.this.connector.connect();
                    b.setText(UIController.this.rb.getString("disconnect"));
                } else {
                    log.info("User disconnect");
                    UIController.this.connector.disconnect();
                    b.setText(UIController.this.rb.getString("connect"));
                }
            }
        }
    }

    public class CloseListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            log.info("User close");
            closeApplication();
        }
    }

    public void showAdvancedOptions() {
        this.conView.showAdvancedOptions();
    }

    public void checkForValidCustomServerAddressAndUpdate() {
        if ( this.conView.isCustomAddressSelected() ) {
            String ip = this.conView.getCustomIP();
            String port = this.conView.getCustomPort();
            if ( ip != null && port != null && !ip.equals("") && !port.equals("") ) {
                String address = ip + ":" + port;
                log.info("Valid custom address " + address);
                this.connector.updateCustomServerAddress(address);
            } else {
                log.info("Invalid custom address (null or empty) - Using default address");
                this.connector.resetToDefaultServerAddress();
            }
        } else {
            this.connector.resetToDefaultServerAddress();
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void closeApplication() {
        if ( this.connected ) {
            String[] buttons = {
                this.rb.getString("close"),
                this.rb.getString("cancel")
            };
            int n =
                ORAPopup.showPopup(
                    this.conView,
                    this.rb.getString("attention"),
                    this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(getClass().getClassLoader().getResource("Roberta.png")),
                    buttons);
            if ( n == 0 ) {
                this.connector.close();
                Main.stopFileLogger();
                System.exit(0);
            }
        } else {
            Main.stopFileLogger();
            System.exit(0);
        }
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        Connector.State state = (Connector.State) arg1;
        switch ( state ) {
            case WAIT_FOR_CONNECT:
                //this.conView.setNew(this.connector.getBrickName());
                this.conView.setWaitForConnect();
                break;
            case WAIT_FOR_SERVER:
                this.conView.setNew(this.rb.getString("token") + " " + this.connector.getToken());
                break;
            case WAIT_FOR_CMD:
                this.connected = true;
                this.conView.setNew(this.rb.getString("name") + " " + this.connector.getBrickName());
                this.conView.setWaitForCmd();
                break;
            case DISCOVER:
                this.connected = false;
                this.conView.setDiscover();
                break;
            case WAIT_EXECUTION:
                this.conView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("restartInfo"), null);
                break;
            case UPDATE_FAIL:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("updateFail"), null);
                break;
            case ERROR_HTTP:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("httpErrorInfo"), null);
                break;
            case ERROR_BRICK:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("httpBrickInfo"), null);
                break;
            case TOKEN_TIMEOUT:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("tokenTimeout"), null);
            default:
                break;
        }
    }

    private void showAboutPopup() {
        ORAPopup.showPopup(
            this.conView,
            this.rb.getString("about"),
            this.rb.getString("aboutInfo"),
            new ImageIcon(
                new ImageIcon(getClass().getClassLoader().getResource("iais_logo.gif"))
                    .getImage()
                    .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
    }
}
