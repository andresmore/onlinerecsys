package edu.uniandes.privateRecsys.onlineRecommender.postgresdb;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.IncrementalDBFactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;

public class PosgresDAO {
	
	
	private static PosgresDAO instance;

	private PoolingDataSource dataSource;
	
	private final static Logger LOG = Logger.getLogger(IncrementalDBFactorUserItemRepresentation.class
		      .getName());

	private PosgresDAO() throws PrivateRecsysException{
		try {
			Properties prop = new Properties();
			 
	    
	               //load a properties file
	    		prop.load(new FileInputStream("data/postgres/db.properties"));
	    	
		      // load the underlying driver
		      try {
		        Class.forName("org.postgresql.Driver");
		      } catch (ClassNotFoundException ex) {
		        LOG.log(Level.SEVERE,"Error loading Postgres driver: " + ex.getMessage(),ex);
		        throw new PrivateRecsysException(ex.getMessage());
		      }
		      // Build the DSN: jdbc:postgresql://host:port/database
		      StringBuilder buf = new StringBuilder();
		      buf.append("jdbc:postgresql://").append(prop.getProperty("db.host")).append(":");
		      buf.append(prop.getProperty("db.port")).append("/");
		      buf.append(prop.getProperty("db.name"));
		      LOG.info("DSN: " + buf.toString());
		 
		     
		 
		      DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(buf.toString(), prop);
		      GenericObjectPool connectionPool = new GenericObjectPool(null);
		     
		      PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
		      this.dataSource = new PoolingDataSource(connectionPool);
		      
		    } catch (Exception ex) {
		      // handle the error
		    	LOG.log(Level.SEVERE,"Error init Postgres pool connection: " + ex.getMessage(),ex);
		    	System.err.println("Got error initializing data source: " + ex.getMessage());
		    	throw new PrivateRecsysException(ex.getMessage());
		    	
		    }
	}

	public static PosgresDAO getInstance() throws PrivateRecsysException {
		if(instance==null)
			instance= new PosgresDAO();
		
		return instance;
	}

	synchronized public boolean containsItemKey(long itemId) throws SQLException {
		boolean resp=false;
		Connection con=null;
		PreparedStatement st=null;
		ResultSet rs=null;
		try{
			con=this.dataSource.getConnection();
			st=con.prepareStatement("select 1 from item where id = ? limit 1");
			st.setLong(1, itemId);
			rs=st.executeQuery();
			while(rs.next())
				resp=true;
			
			
		}catch(SQLException e){
			throw e;
		}finally{
			
			if(con!=null)
				try {
					con.close();
				} catch (SQLException e) {}
		}
		return resp;
	}

	public Vector getItemFactors(long itemId) throws SQLException {
		Vector resp=null;
		Connection con=null;
		PreparedStatement st=null;
		ResultSet rs=null;
		try{
			con=this.dataSource.getConnection();
			st=con.prepareStatement("select factor from item where id = ? limit 1");
			st.setLong(1, itemId);
			rs=st.executeQuery();
			while(rs.next()){
				Array arr=rs.getArray(1);
				BigDecimal[] dec=(BigDecimal[]) arr.getArray();
				double[] data= new double[dec.length];
				for (int i = 0; i < data.length; i++) {
					data[i]=dec[i].doubleValue();
				}
				resp=new DenseVector(data);
			}
			
		}catch(SQLException e){
			throw e;
		}finally{
			if(rs!=null)
				try {
				rs.close();
				} catch (SQLException e) {}
			if(st!=null)
				try {
				st.close();
				} catch (SQLException e) {}
			if(con!=null)
				try {
					con.close();
				} catch (SQLException e) {}
		}
		return resp;
	}

	public void putItem(long itemId, Vector vec) throws SQLException {
		Connection con=null;
		PreparedStatement st=null;
		try {
			con = this.dataSource.getConnection();
			Object[] vecDouble = new Object[vec.size()];
			for (int i = 0; i < vecDouble.length; i++) {
				vecDouble[i] = vec.get(i);
			}
			Array sqlArray = con.createArrayOf("float8", vecDouble);
			st=con.prepareStatement("insert into item(id,factor,timestamp) values(?,?,?)");
			st.setLong(1, itemId);
			st.setArray(2, sqlArray);
			st.setLong(3, System.currentTimeMillis());
			st.executeUpdate();
			
		} catch (SQLException e) {
			throw e;
		}finally{
			
				try{
					if(con!=null)
						con.close();
				}catch(Exception e){};
		}
	}
	
	public static void main(String[] args) throws PrivateRecsysException, SQLException {
		PosgresDAO dao= new PosgresDAO();
		Vector vec= new DenseVector(5);
		vec=PrivateRandomUtils.normalRandom(0, 1, vec);
		vec=VectorProjector.projectVectorIntoSimplex(vec);
		dao.putItem(1, vec);
	}

	public void updateItem(long itemId, Vector vec) throws SQLException {
		Connection con=null;
		PreparedStatement st=null;
		try {
			con = this.dataSource.getConnection();
			Object[] vecDouble = new Object[vec.size()];
			for (int i = 0; i < vecDouble.length; i++) {
				vecDouble[i] = vec.get(i);
			}
			Array sqlArray = con.createArrayOf("float8", vecDouble);
			st=con.prepareStatement("update item set factor=?, timestamp=? where id=?");
			
			st.setArray(1, sqlArray);
			st.setLong(2, System.currentTimeMillis());
			st.setLong(3, itemId);
			st.executeUpdate();
			
		} catch (SQLException e) {
			throw e;
		}finally{
			
				try{
					if(con!=null)
						con.close();
				}catch(Exception e){};
		}
		
	}

	public boolean containsUserKey(long userId) throws SQLException {
		boolean resp=false;
		Connection con=null;
		PreparedStatement st=null;
		ResultSet rs=null;
		try{
			con=this.dataSource.getConnection();
			st=con.prepareStatement("select 1 from plistauser where id = ? limit 1");
			st.setLong(1, userId);
			rs=st.executeQuery();
			while(rs.next())
				resp=true;
			
			
		}catch(SQLException e){
			throw e;
		}finally{
			
			if(con!=null)
				try {
					con.close();
				} catch (SQLException e) {}
		}
		return resp;
	}

	public LinkedList<Vector> getUserFactors(RatingScale ratingScale, long userId) throws SQLException {
		// TODO Generalizar para escala de varios factores
		
		LinkedList<Vector> resp=new LinkedList<Vector>();
		Connection con=null;
		PreparedStatement st=null;
		ResultSet rs=null;
		String factorKey= "";
		String[]scale=ratingScale.getScale();
		for (int i = 0; i < scale.length; i++) {
			factorKey+="factor"+scale[i]+",";
		}
		factorKey=factorKey.substring(0, factorKey.length()-1);
		try{
			con=this.dataSource.getConnection();
			st=con.prepareStatement("select "+factorKey+" from plistauser where id = ? limit 1");
			st.setLong(1, userId);
			rs=st.executeQuery();
			while(rs.next()){
				for (int arPos = 1; arPos <= ratingScale.getRatingSize(); arPos++) {
					Array arr = rs.getArray(arPos);
					BigDecimal[] dec = (BigDecimal[]) arr.getArray();
					double[] data = new double[dec.length];
					for (int i = 0; i < data.length; i++) {
						data[i] = dec[i].doubleValue();
					}
					resp.add(new DenseVector(data));
				}
			}
			
		}catch(SQLException e){
			throw e;
		}finally{
			if(rs!=null)
				try {
				rs.close();
				} catch (SQLException e) {}
			if(st!=null)
				try {
				st.close();
				} catch (SQLException e) {}
			if(con!=null)
				try {
					con.close();
				} catch (SQLException e) {}
		}
		return resp;
	}

	public void insertUser(long userId,HashMap<String, Vector> userProfile) throws SQLException {
		Connection con=null;
		PreparedStatement st=null;
		try {
			con = this.dataSource.getConnection();
			String factorKey= "";
			String factorQuestions= "";
			LinkedList<Object[]> factorObjects= new LinkedList<Object[]>();
			for (String key : userProfile.keySet()) {
				Vector vec=userProfile.get(key);
				Object[] vecDouble = new Object[vec.size()];
				for (int i = 0; i < vecDouble.length; i++) {
					vecDouble[i] = vec.get(i);
				}
				factorKey+="factor"+key+",";
				factorQuestions+="?,";
				factorObjects.add(vecDouble);
				
					
			}
			
			
			st=con.prepareStatement("insert into plistauser("+factorKey+"id) values("+factorQuestions+"?)");
			int pos=1;
			
			for (int i = 0; i < factorObjects.size(); i++) {
				Array sqlArray = con.createArrayOf("float8", factorObjects.get(i));
				st.setArray(pos++, sqlArray);
			}
			st.setLong(pos++, userId);
			
			st.executeUpdate();
			
		} catch (SQLException e) {
			throw e;
		}finally{
			
				try{
					if(con!=null)
						con.close();
				}catch(Exception e){};
		}
		
	}

	public void updateUser(long userId, HashMap<String, Vector> userProfile) throws SQLException {
		Connection con=null;
		PreparedStatement st=null;
		try {
			con = this.dataSource.getConnection();
			String factorKey= "";
			LinkedList<Object[]> factorObjects= new LinkedList<Object[]>();
			for (String key : userProfile.keySet()) {
				Vector vec=userProfile.get(key);
				Object[] vecDouble = new Object[vec.size()];
				for (int i = 0; i < vecDouble.length; i++) {
					vecDouble[i] = vec.get(i);
				}
				factorKey+="factor"+key+"=?,";
				factorObjects.add(vecDouble);
				
					
			}
			factorKey=factorKey.substring(0, factorKey.length()-1);
			
			
			st=con.prepareStatement("update plistauser set "+factorKey+" where id=?");
			int pos=1;
			
			for (int i = 0; i < factorObjects.size(); i++) {
				Array sqlArray = con.createArrayOf("float8", factorObjects.get(i));
				st.setArray(pos++, sqlArray);
			}
			st.setLong(pos++, userId);
			
			st.executeUpdate();
			
		} catch (SQLException e) {
			throw e;
		}finally{
			
				try{
					if(con!=null)
						con.close();
				}catch(Exception e){};
		}
		
	}

}
