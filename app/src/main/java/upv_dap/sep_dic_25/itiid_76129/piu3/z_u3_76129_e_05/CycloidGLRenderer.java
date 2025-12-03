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
    private float cameraDistance = 400f;
    
    // Animación de zoom de cámara
    private float targetCameraDistance = 400f;
    private static final float CAMERA_ZOOM_SPEED = 0.08f; // Velocidad de transición suave

    // Parámetros de animación
    private double radius = 50.0;
    private double currentTheta = 0.0;
    private boolean isAnimating = false;
    private boolean animationComplete = false;
    private static final double THETA_INCREMENT = 0.02;
    private static final double TWO_PI = 2 * Math.PI;
    
    // Animación de brillo del área al completar (efecto único)
    private float areaGlowAlpha = 0.0f;
    private boolean areaGlowActive = false;
    private boolean areaGlowFadingOut = false;
    private static final float GLOW_FADE_IN_SPEED = 0.04f;
    private static final float GLOW_FADE_OUT_SPEED = 0.015f;
    private static final float GLOW_HOLD_TIME = 60f; // Frames para mantener el brillo
    private float glowHoldCounter = 0f;

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
        // Far plane muy grande para soportar radios hasta 10000
        // El cicloide con radio 10000 se extiende ~62832 unidades en X
        // Necesitamos far plane de al menos 150000 para verlo completo
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 500000);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Actualizar animación de zoom de cámara (transición suave)
        updateCameraZoom();
        
        // Actualizar animación de brillo del área
        updateAreaGlow();

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

        // Solo dibujar la rueda si hay animación activa o completada
        if (isAnimating || animationComplete) {
            drawCircle();
            drawSpokes();
            drawTracerPoint();
        }
    }

    /**
     * Actualiza la animación suave del zoom de cámara
     * Interpola gradualmente hacia la distancia objetivo
     */
    private void updateCameraZoom() {
        if (Math.abs(cameraDistance - targetCameraDistance) > 0.5f) {
            // Interpolación suave (lerp) hacia la distancia objetivo
            cameraDistance += (targetCameraDistance - cameraDistance) * CAMERA_ZOOM_SPEED;
        } else {
            cameraDistance = targetCameraDistance;
        }
    }
    
    /**
     * Actualiza la animación de brillo del área
     * Crea un efecto de destello único: fade-in -> hold -> fade-out
     */
    private void updateAreaGlow() {
        if (animationComplete && !areaGlowActive && !areaGlowFadingOut && areaGlowAlpha == 0.0f) {
            // Iniciar animación de brillo cuando se completa el cicloide
            areaGlowActive = true;
            areaGlowAlpha = 0.0f;
            glowHoldCounter = 0f;
        }
        
        if (areaGlowActive) {
            if (!areaGlowFadingOut) {
                // Fase de entrada suave (fade in)
                if (areaGlowAlpha < 1.0f) {
                    areaGlowAlpha += GLOW_FADE_IN_SPEED;
                    if (areaGlowAlpha > 1.0f) areaGlowAlpha = 1.0f;
                } else {
                    // Mantener el brillo por un momento
                    glowHoldCounter++;
                    if (glowHoldCounter >= GLOW_HOLD_TIME) {
                        areaGlowFadingOut = true;
                    }
                }
            } else {
                // Fase de salida suave (fade out)
                areaGlowAlpha -= GLOW_FADE_OUT_SPEED;
                if (areaGlowAlpha <= 0.0f) {
                    areaGlowAlpha = 0.0f;
                    areaGlowActive = false;
                    // No resetear areaGlowFadingOut para que no vuelva a activarse
                }
            }
        }
    }

    /**
     * Configura la posición y rotación de la cámara
     * Permite vista orbital completa alrededor del cicloide
     */
    private void setupCamera() {
        // Calcular posición de cámara orbital
        float radX = (float) Math.toRadians(cameraRotationX);
        float radY = (float) Math.toRadians(cameraRotationY);

        // Posición de la cámara en coordenadas esféricas
        float eyeX = cameraDistance * (float) (Math.cos(radX) * Math.sin(radY));
        float eyeY = cameraDistance * (float) Math.sin(radX);
        float eyeZ = cameraDistance * (float) (Math.cos(radX) * Math.cos(radY));

        // Centro del cicloide (punto al que mira la cámara)
        float centerX = (float) (Math.PI * radius);
        float centerY = (float) radius;
        float centerZ = 0f;

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX + centerX, eyeY + centerY, eyeZ + centerZ,  // Posición cámara
                centerX, centerY, centerZ,                        // Punto de mira
                0f, 1f, 0f);                                      // Vector arriba

        // Sin rotación adicional en el modelo
        Matrix.setIdentityM(modelMatrix, 0);
    }

    /**
     * Actualiza la animación
     */
    private void updateAnimation() {
        if (animationComplete) {
            return; // No continuar si ya se completó
        }

        currentTheta += THETA_INCREMENT;

        if (currentTheta >= TWO_PI) {
            currentTheta = TWO_PI;
            animationComplete = true;
            isAnimating = false;
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

        // Color azul vibrante
        GLES20.glUniform4f(colorHandle, 0.2f, 0.5f, 1.0f, 1.0f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(6f);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, trailPoints.size());

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja la rueda realista con neumático, rin y buje
     */
    private void drawCircle() {
        double centerX = currentTheta * radius;
        double centerY = radius;
        int segments = 64;
        
        GLES20.glUseProgram(shaderProgram);
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        // === 1. NEUMÁTICO EXTERIOR (negro grueso) ===
        float[] tireVertices = new float[(segments + 1) * 3];
        for (int i = 0; i <= segments; i++) {
            double angle = (i / (double) segments) * TWO_PI;
            tireVertices[i * 3] = (float) (centerX + radius * Math.cos(angle));
            tireVertices[i * 3 + 1] = (float) (centerY + radius * Math.sin(angle));
            tireVertices[i * 3 + 2] = 0f;
        }
        FloatBuffer tireBuffer = createFloatBuffer(tireVertices);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, tireBuffer);
        GLES20.glUniform4f(colorHandle, 0.15f, 0.15f, 0.15f, 1.0f); // Negro oscuro
        GLES20.glLineWidth(8f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, segments + 1);
        
        // === 2. RIN EXTERIOR (plateado) ===
        float rimRadius = (float) (radius * 0.85);
        float[] rimVertices = new float[(segments + 1) * 3];
        for (int i = 0; i <= segments; i++) {
            double angle = (i / (double) segments) * TWO_PI;
            rimVertices[i * 3] = (float) (centerX + rimRadius * Math.cos(angle));
            rimVertices[i * 3 + 1] = (float) (centerY + rimRadius * Math.sin(angle));
            rimVertices[i * 3 + 2] = 0f;
        }
        FloatBuffer rimBuffer = createFloatBuffer(rimVertices);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, rimBuffer);
        GLES20.glUniform4f(colorHandle, 0.75f, 0.75f, 0.8f, 1.0f); // Plateado
        GLES20.glLineWidth(3f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, segments + 1);
        
        // === 3. RIN INTERIOR (plateado más oscuro) ===
        float innerRimRadius = (float) (radius * 0.20);
        float[] innerRimVertices = new float[(segments + 1) * 3];
        for (int i = 0; i <= segments; i++) {
            double angle = (i / (double) segments) * TWO_PI;
            innerRimVertices[i * 3] = (float) (centerX + innerRimRadius * Math.cos(angle));
            innerRimVertices[i * 3 + 1] = (float) (centerY + innerRimRadius * Math.sin(angle));
            innerRimVertices[i * 3 + 2] = 0f;
        }
        FloatBuffer innerRimBuffer = createFloatBuffer(innerRimVertices);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, innerRimBuffer);
        GLES20.glUniform4f(colorHandle, 0.6f, 0.6f, 0.65f, 1.0f); // Plateado oscuro
        GLES20.glLineWidth(4f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, segments + 1);
        
        // === 4. BUJE CENTRAL (relleno oscuro) ===
        float hubRadius = (float) (radius * 0.08);
        int hubSegments = 24;
        float[] hubVertices = new float[(hubSegments + 2) * 3]; // +2 para el centro y cierre
        // Centro del buje
        hubVertices[0] = (float) centerX;
        hubVertices[1] = (float) centerY;
        hubVertices[2] = 0f;
        for (int i = 0; i <= hubSegments; i++) {
            double angle = (i / (double) hubSegments) * TWO_PI;
            hubVertices[(i + 1) * 3] = (float) (centerX + hubRadius * Math.cos(angle));
            hubVertices[(i + 1) * 3 + 1] = (float) (centerY + hubRadius * Math.sin(angle));
            hubVertices[(i + 1) * 3 + 2] = 0f;
        }
        FloatBuffer hubBuffer = createFloatBuffer(hubVertices);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, hubBuffer);
        GLES20.glUniform4f(colorHandle, 0.3f, 0.3f, 0.35f, 1.0f); // Gris oscuro
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, hubSegments + 2);
        
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
    
    /**
     * Helper para crear FloatBuffer
     */
    private FloatBuffer createFloatBuffer(float[] vertices) {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(vertices);
        buffer.position(0);
        return buffer;
    }

    /**
     * Dibuja los radios de la rueda estilo bicicleta
     * Con patrón cruzado realista
     */
    private void drawSpokes() {
        double centerX = currentTheta * radius;
        double centerY = radius;

        int numSpokes = 16; // Número de radios por lado
        float innerRadius = (float) (radius * 0.10); // Desde el buje
        float outerRadius = (float) (radius * 0.83); // Hasta el rin
        
        // Crear radios con patrón cruzado (como rueda de bicicleta real)
        List<Float> spokeVertices = new ArrayList<>();
        
        for (int i = 0; i < numSpokes; i++) {
            // Radio que va hacia la derecha (cruzado)
            double angle1 = -currentTheta + (i * TWO_PI / numSpokes);
            double angle2 = -currentTheta + ((i + 2) * TWO_PI / numSpokes); // Cruzado 2 posiciones
            
            // Desde el buje
            spokeVertices.add((float) (centerX + innerRadius * Math.cos(angle1)));
            spokeVertices.add((float) (centerY + innerRadius * Math.sin(angle1)));
            spokeVertices.add(0f);
            
            // Hasta el rin (cruzado)
            spokeVertices.add((float) (centerX + outerRadius * Math.cos(angle2)));
            spokeVertices.add((float) (centerY + outerRadius * Math.sin(angle2)));
            spokeVertices.add(0f);
        }
        
        // Segundo set de radios cruzados en dirección opuesta
        for (int i = 0; i < numSpokes; i++) {
            double angle1 = -currentTheta + ((i + 0.5) * TWO_PI / numSpokes);
            double angle2 = -currentTheta + ((i - 1.5) * TWO_PI / numSpokes);
            
            spokeVertices.add((float) (centerX + innerRadius * Math.cos(angle1)));
            spokeVertices.add((float) (centerY + innerRadius * Math.sin(angle1)));
            spokeVertices.add(0f);
            
            spokeVertices.add((float) (centerX + outerRadius * Math.cos(angle2)));
            spokeVertices.add((float) (centerY + outerRadius * Math.sin(angle2)));
            spokeVertices.add(0f);
        }
        
        float[] vertices = new float[spokeVertices.size()];
        for (int i = 0; i < spokeVertices.size(); i++) {
            vertices[i] = spokeVertices.get(i);
        }

        spokesBuffer = createFloatBuffer(vertices);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, spokesBuffer);

        // Color plateado metálico para los radios
        GLES20.glUniform4f(colorHandle, 0.7f, 0.7f, 0.75f, 0.9f);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glLineWidth(1.5f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertices.length / 3);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Dibuja el punto trazador
     */
    private void drawTracerPoint() {
        if (!isAnimating && !animationComplete) return;

        double centerX = currentTheta * radius;
        double centerY = radius;
        double rotationAngle = -currentTheta;

        float pointX = (float) (centerX + radius * Math.sin(rotationAngle));
        float pointY = (float) (centerY - radius * Math.cos(rotationAngle));

        // Dibujar como un pequeño cuadrado (2 triángulos)
        float size = (float) (radius * 0.2f);
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

        // Color rojo brillante para el punto trazador
        GLES20.glUniform4f(colorHandle, 1.0f, 0.2f, 0.2f, 1.0f);

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
     * Dibuja el área bajo la curva con efecto de brillo al completar
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

        areaBuffer = createFloatBuffer(vertices);

        GLES20.glUseProgram(shaderProgram);

        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, areaBuffer);

        // Calcular color con efecto de brillo
        float baseAlpha = 0.3f;
        
        if (areaGlowActive && areaGlowAlpha > 0.0f) {
            // Efecto de destello único con verde claro brillante
            float glowIntensity = areaGlowAlpha;
            
            // Color verde claro brillante (interpolación suave)
            float r = 0.39f + glowIntensity * 0.3f;   // Añadir algo de luz
            float g = 0.78f + glowIntensity * 0.22f;  // Verde más brillante -> 1.0
            float b = 0.39f + glowIntensity * 0.4f;   // Más cyan/claro
            float a = baseAlpha + glowIntensity * 0.4f; // Más visible durante el brillo
            
            GLES20.glUniform4f(colorHandle, r, g, b, a);
        } else {
            // Color normal
            GLES20.glUniform4f(colorHandle, 0.39f, 0.78f, 0.39f, baseAlpha);
        }

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 3);
        
        // Dibujar capa adicional de brillo verde claro cuando está activo
        if (areaGlowActive && areaGlowAlpha > 0.2f) {
            float glowAlpha = areaGlowAlpha * 0.25f;
            
            // Capa de brillo verde claro/menta encima
            GLES20.glUniform4f(colorHandle, 0.6f, 1.0f, 0.7f, glowAlpha);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.length / 3);
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Inicializa geometría estática
     */
    private void initializeStaticGeometry() {
        // Eje X - extendido para soportar radios grandes
        float[] axisVertices = {
                0f, 0f, 0f,
                (float) (TWO_PI * 10000), 0f, 0f
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
     * Rota la cámara (sistema orbital)
     */
    public void rotateCamera(float deltaX, float deltaY) {
        cameraRotationX += deltaX;
        cameraRotationY += deltaY;

        // Limitar rotación vertical para evitar inversión
        cameraRotationX = Math.max(-89f, Math.min(89f, cameraRotationX));

        // La rotación horizontal puede ser completa (360°)
        if (cameraRotationY > 360f) cameraRotationY -= 360f;
        if (cameraRotationY < 0f) cameraRotationY += 360f;
    }

    /**
     * Ajusta la distancia de la cámara (zoom) con animación suave
     */
    public void setCameraDistance(float distance) {
        this.targetCameraDistance = distance;
    }
    
    /**
     * Ajusta la distancia de la cámara inmediatamente (sin animación)
     */
    public void setCameraDistanceImmediate(float distance) {
        this.cameraDistance = distance;
        this.targetCameraDistance = distance;
    }

    /**
     * Calcula la distancia óptima de la cámara según el radio
     * El cicloide se extiende 2*PI*radius en X, necesitamos ver todo el ancho
     */
    private float calculateOptimalCameraDistance(double radius) {
        // El ancho total del cicloide es 2*PI*radius ≈ 6.28*radius
        // Para ver todo correctamente, usamos un factor de 12
        // Esto asegura que incluso con radio 10000 (ancho ~62832) se vea completo
        return (float) (radius * 12);
    }

    /**
     * Inicia la animación
     */
    public void startAnimation(double radius) {
        this.radius = radius;
        this.currentTheta = 0.0;
        this.isAnimating = true;
        this.animationComplete = false;
        this.trailPoints.clear();
        
        // Resetear animación de brillo
        this.areaGlowActive = false;
        this.areaGlowFadingOut = false;
        this.areaGlowAlpha = 0.0f;
        this.glowHoldCounter = 0f;

        // Calcular y establecer la distancia óptima de la cámara con animación suave
        this.targetCameraDistance = calculateOptimalCameraDistance(radius);
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