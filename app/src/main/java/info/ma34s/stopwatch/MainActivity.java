package info.ma34s.stopwatch;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final int INTERVAL_MS =10;
    private static final int TIMER_TO_MS =1000*1000;

    private TextView elapsedText;
    private Button startButton;
    private Button resetButton;

    private Thread timerThread;
    private final Handler handler = new Handler();

    private long startTime;
    private long elapsedTime;

    class CustomNonConfigurationData
    {
        public long startTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        elapsedText = findViewById(R.id.elapsed_text);

        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this);

        resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(this);
        timerThread = new Thread();
        reset_timer();

        CustomNonConfigurationData data = (CustomNonConfigurationData)getLastCustomNonConfigurationInstance();
        if (data != null) {
            if( data.startTime > 0 )
            {
                start_timer(data.startTime);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(timerThread.isAlive()) {
            //動作中　-> Set Keep screen
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        CustomNonConfigurationData data = new CustomNonConfigurationData();
        if(timerThread.isAlive()) {
            data.startTime = startTime;
        } else {
            data.startTime = -1;
        }
        return data;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timerThread.isAlive()) {
            stopAndWaitThread();
        }
    }

    @Override
    public void onClick(View v) {

        if (v == startButton) {
            if(timerThread.isAlive()) {
                stop_timer();//動作中　->停止させる
            } else {
                start_timer();//停止中 ->開始させる
            }
        } else if (v == resetButton) {
            reset_timer();
        } else {
            //do nothing
        }
    }
    //--------------------
    // Timer control
    //--------------------
    private void start_timer(long startTime)
    {
        startThread();
        //再開時には取得時間に経過時間分Offsetする
        //startTime = System.currentTimeMillis() - elapsedTime;
        this.startTime = startTime;
        startButton.setText(R.string.stop);
        resetButton.setEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void start_timer() {
        start_timer(System.nanoTime() - elapsedTime);
    }

    private void stop_timer() {
        stopAndWaitThread();//UIスレッドで同期で待つ

        startButton.setText(R.string.restart);
        resetButton.setEnabled(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void reset_timer() {
        elapsedTime = 0;
        setElapsedTime(elapsedTime);
        startButton.setText(R.string.start);
        resetButton.setEnabled(false);
    }


    //--------------------
    // Timer result UI
    //--------------------
    private void setElapsedTime(long time) {
        final SimpleDateFormat dataFormat =
                new SimpleDateFormat("mm:ss.SS", Locale.JAPAN);
        elapsedText.setText(dataFormat.format(time / TIMER_TO_MS));
    }

    private final Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            elapsedTime = (System.nanoTime() - startTime);
            setElapsedTime(elapsedTime);
        }
    };
    //--------------------
    // Thread
    //--------------------
    private void startThread() {
        timerThread = new Thread(timerThreadRunnable);//Threadは使いまわしできないので、必ずnewする
        timerThread.start();
    }
    private void stopAndWaitThread() {
        timerThread.interrupt();
        try {
            timerThread.join(INTERVAL_MS * 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private final Runnable timerThreadRunnable = new Runnable() {
        private volatile boolean cancel = false;
        @Override
        public void run() {
            cancel = false;
            while (!cancel) {
                try {
                    Thread.sleep(INTERVAL_MS );
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    cancel = true;
                }
                handler.post(updateUIRunnable);
            }
        }
    };
}