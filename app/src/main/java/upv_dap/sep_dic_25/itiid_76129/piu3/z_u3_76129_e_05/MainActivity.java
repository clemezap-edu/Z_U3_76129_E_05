package upv_dap.sep_dic_25.itiid_76129.piu3.z_u3_76129_e_05;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity - Actividad principal de la aplicación de Cicloide
 *
 * Gestiona la UI y coordina la visualización del cicloide y el cálculo del área.
 * Implementa el patrón MVC separando la lógica de presentación del renderizado.
 */
public class MainActivity extends AppCompatActivity {

    // Componentes de la UI
    private EditText etRadius;
    private Button btnCalculate;
    private TextView tvResult;
    private CycloidGLView cycloidView;

    // Constante matemática PI
    private static final double PI = Math.PI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar componentes de la UI
        initializeViews();

        // Configurar listeners
        setupListeners();
    }

    /**
     * Inicializa las referencias a los componentes de la UI
     */
    private void initializeViews() {
        etRadius = findViewById(R.id.etRadius);
        btnCalculate = findViewById(R.id.btnCalculate);
        tvResult = findViewById(R.id.tvResult);
        cycloidView = findViewById(R.id.cycloidView);

        // Valor por defecto
        etRadius.setText("50");
    }

    /**
     * Configura los listeners de eventos
     */
    private void setupListeners() {
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVisualization();
            }
        });
    }

    /**
     * Inicia la visualización y cálculo del área
     */
    private void startVisualization() {
        // Obtener el radio ingresado por el usuario
        String radiusStr = etRadius.getText().toString().trim();

        // Validación de entrada
        if (radiusStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese un radio", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double radius = Double.parseDouble(radiusStr);

            // Validación de rango
            if (radius <= 0 || radius > 200) {
                Toast.makeText(this, "El radio debe estar entre 1 y 200", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calcular el área teórica: A = 3πa²
            double area = calculateTheoreticalArea(radius);

            // Mostrar resultado
            displayResult(radius, area);

            // Iniciar animación en el custom view
            cycloidView.startAnimation(radius);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingrese un número válido", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calcula el área teórica bajo el cicloide
     * Fórmula: A = 3πa²
     *
     * @param radius Radio del círculo generador
     * @return Área calculada
     */
    private double calculateTheoreticalArea(double radius) {
        return 3 * PI * radius * radius;
    }

    /**
     * Muestra el resultado del cálculo en la UI
     *
     * @param radius Radio utilizado
     * @param area Área calculada
     */
    private void displayResult(double radius, double area) {
        String result = String.format(
                "Radio (a): %.2f\n" +
                        "Área calculada: %.2f unidades²\n" +
                        "Fórmula: A = 3πa²\n" +
                        "Valor de π: %.6f",
                radius, area, PI
        );
        tvResult.setText(result);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar animación cuando la actividad no es visible
        if (cycloidView != null) {
            cycloidView.pauseAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reanudar animación si es necesario
        if (cycloidView != null) {
            cycloidView.resumeAnimation();
        }
    }
}