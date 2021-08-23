package directory.security;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.MarkerFactory;

import directory.Directory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


public class JwtTokenUtil {

	public static final long JWT_TOKEN_VALIDITY_DEFAULT = 60; // this is minutes
	public static final int COOKIE_VALIDITY_WEEK = 3600; // this is seconds
	private static final File SECRET_FILE = new File("./jwt-secret");
	static {
		try {
		if(!SECRET_FILE.exists()){
			//SECRET_FILE.createNewFile();
			FileWriter writer = new FileWriter(SECRET_FILE);
			writer.append(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()));
			writer.close();
		}
		} catch (IOException e) {
			Directory.LOGGER.error(e.toString());
		}
	}
	
	private JwtTokenUtil() {
		super();
	}
	
	/*public static String generateToken(String username, long validityMinutes, Map<String,Object> claims) {
		String jwt = null;
		try{
			jwt = Jwts.builder().setSubject(username)
					.setIssuedAt(new Date(System.currentTimeMillis()))
					.setClaims(claims)
					.setExpiration(Date.from(ZonedDateTime.now().plusMinutes(validityMinutes).toInstant()))
					.signWith(SignatureAlgorithm.HS512, Files.readString(SECRET_FILE.toPath())).compact();
		}catch(Exception e) {
			// throw exception
			e.printStackTrace();
		}
		return jwt;
	}*/
	public static String generateToken(String subject, long validity, Map<String, Object> claims) {
		try {
			return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
					.setExpiration(Date.from(ZonedDateTime.now().plusMinutes(validity).toInstant()))
					.signWith(SignatureAlgorithm.HS512, Files.readString(SECRET_FILE.toPath())).compact();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String generateToken(String username, long validityMinutes) {
		Map<String, Object> claims = new HashMap<>();
		return  generateToken(username, validityMinutes, claims);
	}
	
	public static String generateToken(String username) {
		return generateToken(username,JWT_TOKEN_VALIDITY_DEFAULT);
	}


	
	/**
	 * Token validation 
	 **/
	

	
	public static Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}
	
	public static String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	private static Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	private static <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
			final Claims claims = getAllClaimsFromToken(token);
			return claimsResolver.apply(claims);
		}
	  
	private static Claims getAllClaimsFromToken(String token) {
		try {
			return Jwts.parser().setSigningKey(Files.readString(SECRET_FILE.toPath())).parseClaimsJws(token).getBody();
		}catch(Exception e) {
			Directory.LOGGER.debug(MarkerFactory.getMarker("[SECURITY]"), e.toString());
		}
		return null;
	}
}