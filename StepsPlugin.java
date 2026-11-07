package de.arise.gym;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "Steps",
    permissions = { @Permission(strings = { Manifest.permission.ACTIVITY_RECOGNITION }, alias = "activity") }
)
public class StepsPlugin extends Plugin {

    @PluginMethod
    public void read(PluginCall call) {
        if (getPermissionState("activity") != PermissionState.GRANTED) {
            requestPermissionForAlias("activity", call, "permResult");
            return;
        }
        doRead(call);
    }

    @PermissionCallback
    private void permResult(PluginCall call) {
        if (getPermissionState("activity") == PermissionState.GRANTED) {
            doRead(call);
        } else {
            call.reject("permission");
        }
    }

    private void doRead(final PluginCall call) {
        final SensorManager sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            call.reject("nosensor");
            return;
        }
        final boolean[] done = { false };
        final SensorEventListener[] holder = new SensorEventListener[1];
        holder[0] = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (done[0]) return;
                done[0] = true;
                sm.unregisterListener(holder[0]);
                JSObject result = new JSObject();
                result.put("steps", (double) event.values[0]);
                call.resolve(result);
            }

            @Override
            public void onAccuracyChanged(Sensor s, int accuracy) {}
        };
        sm.registerListener(holder[0], sensor, SensorManager.SENSOR_DELAY_NORMAL);
        new Handler(Looper.getMainLooper()).postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    if (done[0]) return;
                    done[0] = true;
                    sm.unregisterListener(holder[0]);
                    call.reject("timeout");
                }
            },
            5000
        );
    }
}
