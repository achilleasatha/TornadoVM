package tornado.runtime.api;

import tornado.api.Event;
import tornado.common.DeviceObjectState;
import tornado.common.TornadoDevice;

import static tornado.runtime.TornadoRuntime.getTornadoRuntime;

public class LocalObjectState {

    private boolean streamIn;
    private boolean streamOut;

    private GlobalObjectState global;
    private DeviceObjectState device;

    public LocalObjectState(Object object) {
        global = getTornadoRuntime().resolveObject(object);
        device = null;
        streamIn = false;
        streamOut = false;
    }

    public boolean isStreamIn() {
        return streamIn;
    }

    public void setStreamIn(boolean streamIn) {
        this.streamIn = streamIn;
    }

    public boolean isStreamOut() {
        return streamOut;
    }

    public void setStreamOut(boolean streamOut) {
        this.streamOut = streamOut;
    }

    public boolean isModified() {
        return device.isModified();
    }

    public void setModified(boolean modified) {
        device.setModified(modified);
    }

    public boolean isShared() {
        return global.isShared();
    }

    public boolean isValid() {
        return (device == null) ? false : device.isValid();
    }

    public void setValid(boolean valid) {
        device.setValid(valid);
    }

    public boolean isExclusive() {
        return global.isExclusive();
    }

    public TornadoDevice getOwner() {
        return global.getOwner();
    }

    public GlobalObjectState getGlobalState() {
        return global;
    }

    public void setOwner(TornadoDevice owner) {
        global.setOwner(owner);
    }

    public Event sync(Object object) {
//        if (isModified()) {
        TornadoDevice owner = getOwner();
        int eventId = owner.streamOut(object, device, null);
        return owner.resolveEvent(eventId);
//        }
//        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(streamIn ? "SI" : "--");
        sb.append(streamOut ? "SO" : "--");
        sb.append(" ");

        if (device != null) {
            sb.append(device.toString()).append(" ");
        }
        sb.append(global.toString()).append(" ");
        return sb.toString();
    }
}
