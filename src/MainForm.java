import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MainForm extends JFrame {
    private JPanel mainPanel;
    private JButton btn_start;
    private JButton btn_stop;
    private JLabel txt_mon;
    private JLabel txt_tue;
    private JLabel txt_wed;
    private JLabel txt_thu;
    private JLabel txt_fri;
    private JLabel txt_sat;
    private JLabel txt_sun;
    private JLabel txt_total;
    private JLabel txt_remained;
    private Timer timer;
    private TimerTask timerTask;
    private AtomicInteger secLeft;
    private final int totalSecForWork = 40 * 3600;

    public MainForm() {
        setContentPane(mainPanel);
        setTitle("Учет времени");
        setSize(260, 400);
        setResizable(false);
        setVisible(true);

        try {
            BufferedReader reader = new BufferedReader(new FileReader("SpendTime"));

            String line;
            while ((line = reader.readLine()) != null) {
                loadData(line);
            }

            reader.close();
        } catch (IOException ex) {

        }

        calcTotalTime();

        btn_start.addActionListener(e -> {
            activateStartButton(false);

            if (LocalDateTime.now().getDayOfWeek().equals(DayOfWeek.MONDAY)) {
                checkAndClearOldData();
            }

            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    LocalDateTime now = LocalDateTime.now();

                    if (now.getHour() == 0 && now.getMinute() == 0 && now.getSecond() == 0) {
                        secLeft.set(0);
                    }

                    switch (now.getDayOfWeek()) {
                        case MONDAY -> txt_mon.setText(timeToStr(secLeft.incrementAndGet()));
                        case TUESDAY -> txt_tue.setText(timeToStr(secLeft.incrementAndGet()));
                        case WEDNESDAY -> txt_wed.setText(timeToStr(secLeft.incrementAndGet()));
                        case THURSDAY -> txt_thu.setText(timeToStr(secLeft.incrementAndGet()));
                        case FRIDAY -> txt_fri.setText(timeToStr(secLeft.incrementAndGet()));
                        case SATURDAY -> txt_sat.setText(timeToStr(secLeft.incrementAndGet()));
                        case SUNDAY -> txt_sun.setText(timeToStr(secLeft.incrementAndGet()));
                    }

                    calcTotalTime();

                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter("SpendTime"));
                        writer.write(getStringForSave());

                        writer.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };

            setPrevValue();
            timer.scheduleAtFixedRate(timerTask, 0, 1000);
        });

        btn_stop.addActionListener(e -> {
            activateStartButton(true);

            timer.cancel();
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                if (timer != null) {
                    timer.cancel();
                }

                System.exit(0);
            }
        });
    }

    private void activateStartButton(boolean enable) {
        btn_start.setEnabled(enable);
        btn_stop.setEnabled(!enable);
    }

    private void checkAndClearOldData() {
        List<JLabel> labels = Arrays.asList(txt_tue, txt_wed, txt_thu, txt_fri, txt_sat, txt_sun, txt_total);

        for (JLabel label : labels) {
            if (!label.getText().trim().isEmpty()) {
                label.setText("");
            }
        }
    }

    private String timeToStr(int secLeft) {
        int hour = secLeft / 3600;
        int min = (secLeft - hour * 3600) / 60;
        int sec = secLeft % 60;

        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    private void setPrevValue() {
        switch (LocalDateTime.now().getDayOfWeek()) {
            case MONDAY -> secLeft = new AtomicInteger(prevValue(txt_mon.getText()));
            case TUESDAY -> secLeft = new AtomicInteger(prevValue(txt_tue.getText()));
            case WEDNESDAY -> secLeft = new AtomicInteger(prevValue(txt_wed.getText()));
            case THURSDAY -> secLeft = new AtomicInteger(prevValue(txt_thu.getText()));
            case FRIDAY -> secLeft = new AtomicInteger(prevValue(txt_fri.getText()));
            case SATURDAY -> secLeft = new AtomicInteger(prevValue(txt_sat.getText()));
            case SUNDAY -> secLeft = new AtomicInteger(prevValue(txt_sun.getText()));
        }
    }

    private int getTotalSec() {
        int sec = prevValue(txt_mon.getText());
        sec += prevValue(txt_tue.getText());
        sec += prevValue(txt_wed.getText());
        sec += prevValue(txt_thu.getText());
        sec += prevValue(txt_fri.getText());
        sec += prevValue(txt_sat.getText());
        sec += prevValue(txt_sun.getText());

        return sec;
    }

    private int prevValue(String str) {
        if (str.trim().isEmpty()) {
            return 0;
        }

        String[] times = str.split(":");

        int hour = Integer.parseInt(times[0]);
        int min = Integer.parseInt(times[1]);
        int sec = Integer.parseInt(times[2]);

        return hour * 3600 + min * 60 + sec;
    }

    private String getStringForSave() {
        return String.format("MON - %s\n", getTextForSave(txt_mon.getText())) +
                String.format("TUE - %s\n", getTextForSave(txt_tue.getText())) +
                String.format("WED - %s\n", getTextForSave(txt_wed.getText())) +
                String.format("THU - %s\n", getTextForSave(txt_thu.getText())) +
                String.format("FRI - %s\n", getTextForSave(txt_fri.getText())) +
                String.format("SAT - %s\n", getTextForSave(txt_sat.getText())) +
                String.format("SUN - %s", getTextForSave(txt_sun.getText()));
    }

    private String getTextForSave(String text) {
        return text.trim().isEmpty() ? timeToStr(0) : text;
    }

    private void loadData(String line) {
        String[] columns = line.split(" - ");
        String time = columns[1].trim();

        if (time.equals("00:00:00")) {
            time = "";
        }

        switch (columns[0]) {
            case "MON" -> txt_mon.setText(time);
            case "TUE" -> txt_tue.setText(time);
            case "WED" -> txt_wed.setText(time);
            case "THU" -> txt_thu.setText(time);
            case "FRI" -> txt_fri.setText(time);
            case "SAT" -> txt_sat.setText(time);
            case "SUN" -> txt_sun.setText(time);
        }
    }

    private void calcTotalTime() {
        txt_total.setText(timeToStr(getTotalSec()));

        int remain = totalSecForWork - getTotalSec();
        if (remain < 0) {
            remain = 0;
        }

        txt_remained.setText(timeToStr(remain));
    }


}
