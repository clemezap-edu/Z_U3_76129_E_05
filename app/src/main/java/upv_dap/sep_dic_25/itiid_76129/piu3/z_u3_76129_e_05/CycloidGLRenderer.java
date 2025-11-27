package upv_dap.sep_dic_25.itiid_76129.piu3.z_u3_76129_e_05;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * CycloidGLRenderer - Renderer OpenGL ES 2.0 para el cicloide 3D
 */
public class CycloidGLRenderer implements GLSurfaceView.Renderer {

    // Matrices de transformación
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // Rotación de la cámara
    private float cameraRotationX = 30f;
    private float cameraRotationY = 45f;

    // Parámetros de animación
    private double radius = 50.0;
    private double currentTheta = 0.0;
    private boolean isAnimating = false;
    private static final double THETA_INCREMENT = 0.02;
    private static final double TWO_PI = 2 * Math.PI;

    // Buffers de geometría
    private FloatBuffer cycloidTrailBuffer;
    private FloatBuffer circleBuffer;
    private FloatBuffer spokesBuffer;
    private FloatBuffer axisBuffer;
    private FloatBuffer areaBuffer;

    // Lista de puntos del trazo progresivo
    private List<float[]> trailPoints = new ArrayList<>();

    // Programa shader
    private int shaderProgram;

    // Shaders
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Color de fondo
        GLES20.glClearColor(0.95f, 0.95f, 0.95f, 1.0f);

        // Habilitar profundidad
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        // Habilitar blending para transparencias
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Compilar shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        // Inicializar geometría estática
        initializeStaticGeometry();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 1000);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Actualizar animación
        if (isAnimating) {
            updateAnimation();
        }

        // Configurar cámara
        setupCamera();

        // Dibujar escena
        drawAxis();
        drawArea();
        drawCycloidTrail();
        drawCircle();
        drawSpokes();
        drawTracerPoint();
    }

    /**
     * Configura la posición y rotación de la cámara
     */
    private void setupCamera() {
        // Posición de la cámara
        float eyeX = 0f;
        float eyeY = 0f;
        float eyeZ = (float) (radius * 15);

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                0f, 0f, 0f,
                0f, 1f, 0f);

        // Aplicar rotaciones de cámara
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, cameraRotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, cameraRotationY, 0f, 1f, 0f);
    }

    /**
     * Actualiza la animación
     */
    private void updateAnimation() {
        currentTheta += THETA_INCREMENT;

        if (currentTheta >= TWO_PI) {
            currentTheta = 0.0;
            trailPoints.clear();
        }

        // Añadir nuevo punto al trazo
        addTrailPoint();
    }

    /**
     * Añade un punto al trazo del cicloide
     */
    private void addTrailPoint() {
        double centerX = currentTheta * radius;
        double centerY = radius;
        double centerZ = 0;

        double rotationAngle = -currentTheta;
        float x = (float) (centerX + radius * Math.sin(rotationAngle));
        float y = (float) (centerY - radius * Math.cos(rotationAngle));
        float z = 0f;

        trailPoints.add(new float[]{x, y, z});

        // Actualizar buffer del trazo
        updateCycloidTrailBuffer();
    }

    /**
     * Actualiza el buffer del trazo del cicloide
     */
    private void updateCycloidTrailBuffer() {
        if (trailPoints.size() < 2) return;

        float[] vertices = new float[trailPoints.size() * 3];
        for (int i = 0; i < trailPoints.size(); i++) {
            float[] point = trailPoints.get(i);
            vertices[i * 3] = point[0];
            vertices[i * 3 + 1] = point[1];
            vertices[i * 3 + 2] = point[2];
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        cycloidTrailBuffer = bb.asFloatBuffer();
        cycloidTrailBuffer.put(vertices);
        cycloidTrailBuffer.position(0);
    }

    /**
     * Dibuja el trazo del cicloide
     */
    private void drawCycloidTrail() {
        if (cycloidTrailBuffer == null || trailPoints.size() < 2) return;

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, cycloidTrailBuffer);

        // Color azul
        GLES20.glUniform4f(colorHandle, 0f, 0.47f, 0.84f, 1.0f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(8f);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, trailPoints.size());

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja el círculo rodante
     */
    private void drawCircle() {
        double centerX = currentTheta * radius;
        double centerY = radius;

        int segments = 64;
        float[] vertices = new float[(segments + 1) * 3];

        for (int i = 0; i <= segments; i++) {
            double angle = (i / (double) segments) * TWO_PI;
            vertices[i * 3] = (float) (centerX + radius * Math.cos(angle));
            vertices[i * 3 + 1] = (float) (centerY + radius * Math.sin(angle));
            vertices[i * 3 + 2] = 0f;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        circleBuffer = bb.asFloatBuffer();
        circleBuffer.put(vertices);
        circleBuffer.position(0);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, circleBuffer);

        // Color rojo semi-transparente
        GLES20.glUniform4f(colorHandle, 1f, 0.39f, 0.39f, 0.3f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(4f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, segments + 1);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja los radios del círculo
     */
    private void drawSpokes() {
        double centerX = currentTheta * radius;
        double centerY = radius;

        int numSpokes = 12;
        float[] vertices = new float[numSpokes * 6];

        for (int i = 0; i < numSpokes; i++) {
            double angle = -currentTheta + (i * Math.PI / 6);

            vertices[i * 6] = (float) centerX;
            vertices[i * 6 + 1] = (float) centerY;
            vertices[i * 6 + 2] = 0f;

            vertices[i * 6 + 3] = (float) (centerX + radius * Math.sin(angle));
            vertices[i * 6 + 4] = (float) (centerY - radius * Math.cos(angle));
            vertices[i * 6 + 5] = 0f;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        spokesBuffer = bb.asFloatBuffer();
        spokesBuffer.put(vertices);
        spokesBuffer.position(0);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, spokesBuffer);

        GLES20.glUniform4f(colorHandle, 0.6f, 0.6f, 0.6f, 0.5f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(2f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, numSpokes * 2);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja el punto trazador
     */
    private void drawTracerPoint() {
        double centerX = currentTheta * radius;
        double centerY = radius;
        double rotationAngle = -currentTheta;

        float pointX = (float) (centerX + radius * Math.sin(rotationAngle));
        float pointY = (float) (centerY - radius * Math.cos(rotationAngle));

        // Dibujar como un pequeño cuadrado (2 triángulos)
        float size = (float) (radius * 0.15f);
        float[] vertices = {
                pointX - size, pointY - size, 0f,
                pointX + size, pointY - size, 0f,
                pointX + size, pointY + size, 0f,
                pointX - size, pointY + size, 0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(vertices);
        buffer.position(0);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, buffer);

        GLES20.glUniform4f(colorHandle, 0f, 0.47f, 0.84f, 1.0f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja los ejes de coordenadas
     */
    private void drawAxis() {
        if (axisBuffer == null) return;

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, axisBuffer);

        GLES20.glUniform4f(colorHandle, 0.7f, 0.7f, 0.7f, 1.0f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(2f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja el área bajo la curva
     */
    private void drawArea() {
        if (trailPoints.size() < 2) return;

        // Crear triángulos para el área
        List<Float> areaVertices = new ArrayList<>();

        for (int i = 0; i < trailPoints.size() - 1; i++) {
            float[] p1 = trailPoints.get(i);
            float[] p2 = trailPoints.get(i + 1);

            // Triángulo 1
            areaVertices.add(p1[0]);
            areaVertices.add(0f);
            areaVertices.add(0f);

            areaVertices.add(p1[0]);
            areaVertices.add(p1[1]);
            areaVertices.add(0f);

            areaVertices.add(p2[0]);
            areaVertices.add(p2[1]);
            areaVertices.add(0f);

            // Triángulo 2
            areaVertices.add(p1[0]);
            areaVertices.add(0f);
            areaVertices.add(0f);

            areaVertices.add(p2[0]);
            areaVertices.add(p2[1]);
            areaVertices.add(0f);

            areaVertices.add(p2[0]);
            areaVertices.add(0f);
            areaVertices.add(0f);
        }

        float[] vertices = new float[areaVertices.size()];
        for (int i = 0; i < areaVertices.size(); i++) {
            vertices[i] = areaVertices.get(i);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        areaBuffer = bb.asFloatBuffer();
        areaBuffer.put(vertices);
        areaBuffer.position(0);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, areaBuffer);

        GLES20.glUniform4f(colorHandle, 0.39f, 0.78f, 0.39f, 0.3f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 3);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Inicializa geometría estática
     */
    private void initializeStaticGeometry() {
        // Eje X
        float[] axisVertices = {
                0f, 0f, 0f,
                (float) (TWO_PI * 100), 0f, 0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(axisVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        axisBuffer = bb.asFloatBuffer();
        axisBuffer.put(axisVertices);
        axisBuffer.position(0);
    }

    /**
     * Carga un shader
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Rota la cámara
     */
    public void rotateCamera(float deltaX, float deltaY) {
        cameraRotationX += deltaX;
        cameraRotationY += deltaY;

        // Limitar rotación vertical
        cameraRotationX = Math.max(-89f, Math.min(89f, cameraRotationX));
    }

    /**
     * Inicia la animación
     */
    public void startAnimation(double radius) {
        this.radius = radius;
        this.currentTheta = 0.0;
        this.isAnimating = true;
        this.trailPoints.clear();
    }

    /**
     * Pausa la animación
     */
    public void pauseAnimation() {
        isAnimating = false;
    }

    /**
     * Reanuda la animación
     */
    public void resumeAnimation() {
        isAnimating = true;
    }
}