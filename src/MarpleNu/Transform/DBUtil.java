package MarpleNu.Transform;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
	
	public static Connection getConn() throws Exception{

//		String url = "jdbc:mysql://47.114.105.150:3306/apt?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=true";
//		String username = "root";
//		String password = "123456";
		//String driver = "com.mysql.cj.jdbc.Driver";
		//Class.forName(driver);

		//return DriverManager.getConnection(url,username, password);


		Connection conn = null;
		//Statement stmt = null;
		try {
			//1、注册驱动的第二种方法，告诉java我要连接mysql
			//String driver = "com.mysql.cj.jdbc.Driver";
			Class.forName("com.mysql.cj.jdbc.Driver");

			//2、获取连接，告诉java我要连接哪台机器的mysql，并写入用户和密码
			//127.0.0.1和localhost都是本地ip地址
			String url = "jdbc:mysql://47.114.105.150:3306/apt?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=true";
			String user = "root";
			String password = "123456";
			conn = DriverManager.getConnection(url,user,password);
			//System.out.println("good");

			//3、获取数据库操作对象（statement专门执行sql语句）
//			stmt = conn.createStatement();

			//4、执行sql
//			String sql = "insert into dept(deptno,dname,loc) values(50,'军部','长安');";
//			//专门执行DML语句（insert、delete、update）
//			//返回值是“影响数据库中的记录条数”
//			int a = stmt.executeUpdate(sql);
//			System.out.println(a);
//			System.out.println(a == 1 ? "保存成功":"保存失败");

			//5、处理查询结果集
			//插入语句，暂时不需要查询

		} catch (SQLException e) {
			System.out.println("conn failed");
		}
		return conn;
	}
}
