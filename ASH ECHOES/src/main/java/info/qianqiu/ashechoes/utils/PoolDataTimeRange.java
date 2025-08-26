package info.qianqiu.ashechoes.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PoolDataTimeRange {
    private static final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    private Date startTime;
    private Date endTime;

    /**
     * 卡池时间转换
     * @param timeRange
     */
    public PoolDataTimeRange(String timeRange) {
        String[] parts = timeRange.split("~");
        try {
            this.startTime = inputFormat.parse(parts[0].trim());
            this.endTime = inputFormat.parse(parts[1].trim());
        }catch (Exception ignore){
        }
    }

    // getter
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
}