package directory.users;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.servlet.http.Cookie;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


public class JwtTokenUtil {

	public static final long JWT_TOKEN_VALIDITY_DEFAULT = 60; // this is minutes
	public static final int COOKIE_VALIDITY_WEEK = 3600; // this is seconds
	private static  String SECRET ="The bird fights its way out of the egg. The egg is the world. Who would be born must first destroy a world. The bird flies to God. That God's name is Abraxas.";
	private static final File SECRET_FILE = new File("./jwt-secret");
	static {
		try {
		if(!SECRET_FILE.exists()){
			SECRET_FILE.createNewFile();
			FileWriter writer = new FileWriter(SECRET_FILE);
			writer.append(UUID.randomUUID().toString());
			writer.close();
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static String generateToken(String username, long validityMinutes) {
		String jwt = null;
		try{
			jwt = Jwts.builder().setSubject(username).setIssuedAt(new Date(System.currentTimeMillis())).signWith(SignatureAlgorithm.HS512, Files.readAllBytes(SECRET_FILE.toPath())).compact();
		}catch(Exception e) {
			// throw exception
			e.printStackTrace();
		}
		return jwt;
	}
	
	public static String generateToken(String username) {
		return generateToken(username,JWT_TOKEN_VALIDITY_DEFAULT);
	}

	private static String buildToken(String subject, long validity) {
		Map<String, Object> claims = new HashMap<>();
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(Date.from(ZonedDateTime.now().plusMinutes(validity).toInstant()))
				.signWith(SignatureAlgorithm.HS512, SECRET).compact();
	}

	public static Cookie createTokenCookie(String token, Boolean https, int duration) {
		Cookie cookie = new Cookie("jwt", token);
		cookie.setMaxAge(duration);
		cookie.setSecure(false);
		if(https)
			cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		return cookie;
	}
	
	public static Cookie createTokenCookie(String username) {
		String token = generateToken(username);
		Cookie cookie = new Cookie("jwt", token);
		cookie.setMaxAge(COOKIE_VALIDITY_WEEK);
		cookie.setSecure(false);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		return cookie;
	}
	
	public static Cookie createInvalidTokenCookie() {
		Cookie cookie = new Cookie("jwt", null);
		cookie.setMaxAge(0);
		cookie.setSecure(false);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		return cookie;
	}
	
	
	
	/**
	 * Token validation 
	 **/
	
	public static Boolean validateToken(String token, String originalUsername) {
		final String username = getUsernameFromToken(token);
		return (username.equals(originalUsername) && !isTokenExpired(token));
	}
	
	public static Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}
	
	public static Date getTokenExpirationDate(String token) {
		return getExpirationDateFromToken(token);
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
		return Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
	}
}