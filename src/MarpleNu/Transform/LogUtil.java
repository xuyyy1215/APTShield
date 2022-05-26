package MarpleNu.Transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LogUtil {
	
	public static void update(String time, String sum_read, String sum_read_left, String sum_write, String sum_write_left, String sum_net, String sum_net_left, String sum_fork, String sum_fork_left, String sum_clone, String sum_clone_left, String sum_rename, String sum_rename_left, String sum_event, String sum_event_left){
		//插入数据库
		try {
			Connection conn = DBUtil.getConn();
			String sql = "update tb_log_count set time=?, sum_read=?, sum_read_left=?, sum_write=?, sum_write_left=?, sum_net=?, sum_net_left=?, sum_fork=?, sum_fork_left=?, sum_clone=?, sum_clone_left=?, sum_rename=?, sum_rename_left=?, sum_event=?, sum_event_left=? where id = 1;";
			PreparedStatement pstmt = conn.prepareStatement(sql);
//			System.out.println(time);
//			System.out.println(sum_read);
//			System.out.println(sum_read_left);
//			System.out.println(sum_write);
//			System.out.println(sum_write_left);
//			System.out.println(sum_net);
//			System.out.println(sum_net_left);
//			System.out.println(sum_fork);
//			System.out.println(sum_fork_left);
//			System.out.println(sum_clone);
//			System.out.println(sum_clone_left);
//			System.out.println(sum_rename);
//			System.out.println(sum_rename_left);
//			System.out.println(sum_event);
//			System.out.println(sum_event_left);
			pstmt.setString(1, time);
			pstmt.setString(2, sum_read);
			pstmt.setString(3, sum_read_left);
			pstmt.setString(4, sum_write);
			pstmt.setString(5, sum_write_left);
			pstmt.setString(6, sum_net);
			pstmt.setString(7, sum_net_left);
			pstmt.setString(8, sum_fork);
			pstmt.setString(9, sum_fork_left);
			pstmt.setString(10, sum_clone);
			pstmt.setString(11, sum_clone_left);
			pstmt.setString(12, sum_rename);
			pstmt.setString(13, sum_rename_left);
			pstmt.setString(14, sum_event);
			pstmt.setString(15, sum_event_left);
			pstmt.executeUpdate();

			pstmt.close();
			conn.close();
			//System.out.println(" to db");
		}catch (Exception e){
			e.printStackTrace();
			System.out.println("insert failed");
		}
	}

	public static void insert(String time, String log){
		//插入数据库
		try{
//			System.out.println(time);
//			System.out.println(log);
		Connection conn = DBUtil.getConn();
		String sql = "insert into tb_apt_log(is_delete, modify_time, log) value(?,?,?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, 0);
		pstmt.setString(2, time);
		pstmt.setString(3, log);
		pstmt.executeUpdate();
		pstmt.close();
		conn.close();
		}catch (Exception e){

		}
	}
	public static void insert(String time, String host_name, String process, String suspicious_tag, String target_ip, String threat_type){
		//插入数据库
		try{
			Connection conn = DBUtil.getConn();
			String sql = "insert into tb_apt_log(is_delete, modify_time, host_name, process, suspicious_tag, target_ip, threat_type, time) value(?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt = conn.prepareStatement(sql);
//
//			System.out.println(time);
//			System.out.println(host_name);
			pstmt.setInt(1, 0);
			pstmt.setString(2, time);
			pstmt.setString(3, host_name);
			pstmt.setString(4, process);
			pstmt.setString(5, suspicious_tag);
			pstmt.setString(6, target_ip);
			pstmt.setString(7, threat_type);
			pstmt.setString(8, time);
			pstmt.executeUpdate();
			pstmt.close();
			conn.close();
		}catch (Exception e){

		}
	}
}
