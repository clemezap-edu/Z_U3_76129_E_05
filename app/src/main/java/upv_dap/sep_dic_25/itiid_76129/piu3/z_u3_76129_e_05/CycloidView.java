package upv_dap.sep_dic_25.itiid_76129.piu3.z_u3_76129_e_05;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

/**
 * CycloidView - Vista personalizada para renderizar el cicloide
 *
 * Implementa:
 * 1. Dibujo del cicloide completo
 * 2. Animación del círculo rodante
 * 3. Visualización progresiva del área bajo la curva
 * 4. Transformación de coordenadas matemáticas a píxeles
 */
public class CycloidView extends View {

    // Parámetros del cicloide
    private double radius = 50.0;
    private static final double TWO_PI = 2 * Math.PI;

    // Estado de la animación
    private double currentTheta = 0.0;
    private boolean isAnimating = false;
    private boolean isPaused = false;

    // Parámetros de animación
    private static final double THETA_INCREMENT = 0.03; // Velocidad de animación
    private static final int ANIMATION_DELAY = 16; // ~60 FPS

    // Path dinámico para el trazo progresivo
    private Path progressiveCycloidPath;

    // Handler para la animación
    private Handler animationHandler = new Handler();

    // Paints para dibujo
    private Paint cycloidPaint;      // Curva del cicloide
    private Paint circlePaint;       // Círculo rodante
    private Paint circleOutlinePaint; // Contorno del círculo
    private Paint areaPaint;         // Área bajo la curva
    private Paint axisPaint;         // Ejes de coordenadas
    private Paint pointPaint;        // Punto generador

    // Parámetros de transformación de coordenadas
    private float offsetX;
    private float offsetY;
    private float scale;

    // Precalculado del cicloide completo
    private Path cycloidPath;
    private Path trailPath; // Path para la estela que ya pasó

    public CycloidView(Context context) {
        super(context);
        init();
    }

    public CycloidView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CycloidView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Inicializa los objetos Paint y configuraciones
     */
    private void init() {
        // Paint para el cicloide
        cycloidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cycloidPaint.setColor(Color.rgb(0, 120, 215));
        cycloidPaint.setStrokeWidth(5f);
        cycloidPaint.setStyle(Paint.Style.STROKE);
        cycloidPaint.setStrokeCap(Paint.Cap.ROUND);
        cycloidPaint.setStrokeJoin(Paint.Join.ROUND);

        // Paint para el círculo rodante
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.argb(50, 255, 100, 100));
        circlePaint.setStyle(Paint.Style.FILL);

        // Paint para el contorno del círculo
        circleOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleOutlinePaint.setColor(Color.rgb(255, 100, 100));
        circleOutlinePaint.setStrokeWidth(3f);
        circleOutlinePaint.setStyle(Paint.Style.STROKE);

        // Paint para el área bajo la curva
        areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        areaPaint.setColor(Color.argb(80, 100, 200, 100));
        areaPaint.setStyle(Paint.Style.FILL);

        // Paint para los ejes
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.LTGRAY);
        axisPaint.setStrokeWidth(2f);
        axisPaint.setStyle(Paint.Style.STROKE);

        // Paint para el punto generador
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Inicia la animación con un nuevo radio
     *
     * @param newRadius Radio del círculo generador
     */
    public void startAnimation(double newRadius) {
        this.radius = newRadius;
        this.currentTheta = 0.0;
        this.isAnimating = true;
        this.isPaused = false;

        // Calcular parámetros de transformación
        calculateTransformParams();

        // Precalcular el path del cicloide completo
        precalculateCycloidPath();

        // Inicializar el path de la estela
        trailPath = new Path();

        // Iniciar el loop de animación
        animationHandler.post(animationRunnable);
    }

    /**
     * Pausa la animación
     */
    public void pauseAnimation() {
        isPaused = true;
        animationHandler.removeCallbacks(animationRunnable);
    }

    /**
     * Reanuda la animación
     */
    public void resumeAnimation() {
        if (isAnimating && isPaused) {
            isPaused = false;
            animationHandler.post(animationRunnable);
        }
    }

    /**
     * Runnable para el loop de animación
     */
    private Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAnimating && !isPaused) {
                // Incrementar theta
                currentTheta += THETA_INCREMENT;

                // Reiniciar ciclo si se completa
                if (currentTheta >= TWO_PI) {
                    currentTheta = 0.0;
                    trailPath.reset(); // Limpiar estela para reiniciar
                }

                // Actualizar el path progresivo
                updateProgressivePath();

                // Redibujar
                invalidate();

                // Programar siguiente frame
                animationHandler.postDelayed(this, ANIMATION_DELAY);
            }
        }
    };

    /**
     * Calcula los parámetros de transformación para mapear coordenadas
     * matemáticas a coordenadas de pantalla
     */
    private void calculateTransformParams() {
        int width = getWidth();
        int height = getHeight();

        // Ancho del cicloide en unidades matemáticas: 2πa
        double cycloidWidth = TWO_PI * radius;
        // Altura del cicloide: 2a
        double cycloidHeight = 2 * radius;

        // Calcular escala para que el cicloide quepa en la pantalla con margen
        float scaleX = (width * 0.85f) / (float) cycloidWidth;
        float scaleY = (height * 0.6f) / (float) cycloidHeight;
        scale = Math.min(scaleX, scaleY);

        // Calcular offsets para centrar
        offsetX = width * 0.1f;
        offsetY = height * 0.7f; // Posicionar más abajo para ver el área
    }

    /**
     * Precalcula el path completo del cicloide para optimización
     */
    private void precalculateCycloidPath() {
        cycloidPath = new Path();

        // Calcular primer punto
        double x0 = calculateX(0);
        double y0 = calculateY(0);
        float[] screen0 = toScreen(x0, y0);
        cycloidPath.moveTo(screen0[0], screen0[1]);

        // Calcular resto de puntos
        int numPoints = 500; // Alta resolución
        for (int i = 1; i <= numPoints; i++) {
            double t = (i / (double) numPoints) * TWO_PI;
            double x = calculateX(t);
            double y = calculateY(t);
            float[] screen = toScreen(x, y);
            cycloidPath.lineTo(screen[0], screen[1]);
        }
    }

    /**
     * Actualiza el path progresivo del cicloide según el theta actual
     * El punto que traza está en el borde del círculo
     */
    private void updateProgressivePath() {
        // Calcular la posición del punto generador en el borde del círculo
        double centerX = currentTheta * radius; // Centro del círculo se mueve horizontalmente
        double centerY = radius; // Centro siempre a altura 'a'

        // El punto generador está en el borde, inicialmente en la parte inferior
        // Cuando el círculo rueda, el punto rota: ángulo de rotación = -theta (sentido horario)
        double rotationAngle = -currentTheta;

        // Posición del punto generador (que traza el cicloide)
        double tracePointX = centerX + radius * Math.sin(rotationAngle);
        double tracePointY = centerY - radius * Math.cos(rotationAngle);

        // Transformar a coordenadas de pantalla
        float[] screen = toScreen(tracePointX, tracePointY);

        // Si es el primer punto, inicializar el path
        if (currentTheta <= THETA_INCREMENT) {
            trailPath.reset();
            trailPath.moveTo(screen[0], screen[1]);
        } else {
            // Añadir línea al punto actual
            trailPath.lineTo(screen[0], screen[1]);
        }
    }

    /**
     * Calcula la coordenada x del cicloide
     * x(θ) = a(θ - sin(θ))
     */
    private double calculateX(double theta) {
        return radius * (theta - Math.sin(theta));
    }

    /**
     * Calcula la coordenada y del cicloide
     * y(θ) = a(1 - cos(θ))
     */
    private double calculateY(double theta) {
        return radius * (1 - Math.cos(theta));
    }

    /**
     * Transforma coordenadas matemáticas a coordenadas de pantalla
     *
     * @param x Coordenada x matemática
     * @param y Coordenada y matemática
     * @return Array [screenX, screenY]
     */
    private float[] toScreen(double x, double y) {
        float screenX = offsetX + (float) x * scale;
        float screenY = offsetY - (float) y * scale; // Invertir Y
        return new float[]{screenX, screenY};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isAnimating) {
            return;
        }

        // Fondo
        canvas.drawColor(Color.WHITE);

        // Dibujar eje X
        canvas.drawLine(offsetX, offsetY, offsetX + (float)(TWO_PI * radius * scale),
                offsetY, axisPaint);

        // Dibujar el área bajo la curva (hasta el punto actual)
        drawAreaUnderCurve(canvas);

        // Dibujar SOLO la estela progresiva del cicloide (no el cicloide completo)
        if (trailPath != null && !trailPath.isEmpty()) {
            canvas.drawPath(trailPath, cycloidPaint);
        }

        // Dibujar el círculo rodante
        drawRollingCircle(canvas);

        // Dibujar el punto generador
        drawGeneratingPoint(canvas);
    }

    /**
     * Dibuja el área bajo la curva usando el método de trapezoides
     */
    private void drawAreaUnderCurve(Canvas canvas) {
        int numSegments = 100;
        double deltaTheta = currentTheta / numSegments;

        for (int i = 0; i < numSegments; i++) {
            double t1 = i * deltaTheta;
            double t2 = (i + 1) * deltaTheta;

            // Calcular posiciones del punto trazador en ambos momentos
            // Centro del círculo
            double center1X = t1 * radius;
            double center2X = t2 * radius;
            double centerY = radius;

            // Puntos trazadores
            double x1 = center1X + radius * Math.sin(-t1);
            double y1 = centerY - radius * Math.cos(-t1);
            double x2 = center2X + radius * Math.sin(-t2);
            double y2 = centerY - radius * Math.cos(-t2);

            // Transformar a coordenadas de pantalla
            float[] screen1 = toScreen(x1, y1);
            float[] screen2 = toScreen(x2, y2);
            float[] base1 = toScreen(x1, 0);
            float[] base2 = toScreen(x2, 0);

            // Dibujar trapezoide (área infinitesimal)
            Path trapezoid = new Path();
            trapezoid.moveTo(base1[0], base1[1]);
            trapezoid.lineTo(screen1[0], screen1[1]);
            trapezoid.lineTo(screen2[0], screen2[1]);
            trapezoid.lineTo(base2[0], base2[1]);
            trapezoid.close();

            canvas.drawPath(trapezoid, areaPaint);
        }
    }

    /**
     * Dibuja el círculo rodante en la posición actual con rotación visual
     */
    private void drawRollingCircle(Canvas canvas) {
        // Centro del círculo se mueve horizontalmente
        double centerX = currentTheta * radius;
        double centerY = radius; // El centro está siempre a altura 'a'

        float[] screenCenter = toScreen(centerX, centerY);
        float screenRadius = (float) radius * scale;

        // Dibujar círculo relleno
        canvas.drawCircle(screenCenter[0], screenCenter[1], screenRadius, circlePaint);

        // Dibujar contorno
        canvas.drawCircle(screenCenter[0], screenCenter[1], screenRadius, circleOutlinePaint);

        // Dibujar radios para visualizar la rotación
        Paint spokesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spokesPaint.setColor(Color.argb(120, 150, 150, 150));
        spokesPaint.setStrokeWidth(2f);

        // Dibujar 12 radios equidistantes
        for (int i = 0; i < 12; i++) {
            double spokeAngle = -currentTheta + (i * Math.PI / 6);
            double spokeX = centerX + radius * Math.sin(spokeAngle);
            double spokeY = centerY - radius * Math.cos(spokeAngle);
            float[] spokeScreen = toScreen(spokeX, spokeY);
            canvas.drawLine(screenCenter[0], screenCenter[1],
                    spokeScreen[0], spokeScreen[1], spokesPaint);
        }
    }

    /**
     * Dibuja el punto generador del cicloide
     * El punto está en el borde del círculo y traza el cicloide
     */
    private void drawGeneratingPoint(Canvas canvas) {
        // Centro del círculo
        double centerX = currentTheta * radius;
        double centerY = radius;

        // El punto generador está en el borde, rotado según theta
        double rotationAngle = -currentTheta;
        double pointX = centerX + radius * Math.sin(rotationAngle);
        double pointY = centerY - radius * Math.cos(rotationAngle);

        // Transformar a pantalla
        float[] screenPoint = toScreen(pointX, pointY);

        // Dibujar un halo alrededor del punto
        Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaint.setColor(Color.argb(60, 0, 120, 215));
        haloPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(screenPoint[0], screenPoint[1], 18f, haloPaint);

        // Dibujar el punto
        Paint tracerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tracerPaint.setColor(Color.rgb(0, 120, 215));
        tracerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(screenPoint[0], screenPoint[1], 8f, tracerPaint);

        // Borde blanco para destacar
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawCircle(screenPoint[0], screenPoint[1], 8f, borderPaint);
    }
}