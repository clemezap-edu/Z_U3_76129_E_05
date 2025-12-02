package upv_dap.sep_dic_25.itiid_76129.piu3.z_u3_76129_e_05;


import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * CycloidGLView - Vista OpenGL para renderizado 3D del cicloide
 * Soporta interacción táctil para rotar la cámara
 */
public class CycloidGLView extends GLSurfaceView {

    private CycloidGLRenderer renderer;

    // Variables para el control táctil
    private float previousX;
    private float previousY;

    private static final float TOUCH_SCALE_FACTOR = 0.5f; // Más suave y preciso

    public CycloidGLView(Context context) {
        super(context);
        init();
    }

    public CycloidGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Configurar OpenGL ES 2.0
        setEGLContextClientVersion(2);

        // Crear y asignar el renderer
        renderer = new CycloidGLRenderer();
        setRenderer(renderer);

        // Renderizar continuamente para la animación
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Inicia la animación con un nuevo radio
     */
    public void startAnimation(double radius) {
        renderer.startAnimation(radius);
    }

    /**
     * Pausa la animación
     */
    public void pauseAnimation() {
        renderer.pauseAnimation();
    }

    /**
     * Reanuda la animación
     */
    public void resumeAnimation() {
        renderer.resumeAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = x;
                previousY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;

                // Rotar la cámara basado en el movimiento del dedo
                // dx controla rotación horizontal (Y), dy controla rotación vertical (X)
                renderer.rotateCamera(
                        -dy * TOUCH_SCALE_FACTOR,  // Pitch (vertical)
                        dx * TOUCH_SCALE_FACTOR    // Yaw (horizontal)
                );

                previousX = x;
                previousY = y;
                break;
        }

        return true;
    }
}