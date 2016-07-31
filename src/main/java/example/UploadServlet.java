package example;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet(name = "UploadServlet", urlPatterns = { "/upload" })
@MultipartConfig(location = "/tmp", maxFileSize = 1000000, maxRequestSize = 1000000, fileSizeThreshold = 1000000)
public class UploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonArray json = request.getParts().stream().map(part -> {
			System.out.printf("Name:%s; SubmittedFileName:%s; Content-Type:%s; Size:%d;%n", part.getName(),
					part.getSubmittedFileName(), part.getContentType(), part.getSize());
			String digest;
			try {
				digest = digest(part);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return Json.createObjectBuilder().add("name", part.getName())
					.add("submitted-file-name", part.getSubmittedFileName()).add("content-type", part.getContentType())
					.add("size", part.getSize()).add("sha1-checksum", digest).build();
		}).reduce(Json.createArrayBuilder(), (array, part) -> array.add(part), (o1, o2) -> {
			o2.build().stream().forEach(o1::add);
			return o1;
		}).build();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print(json.toString());
	}

	private String digest(Part part) throws IOException {
		try (InputStream in = new BufferedInputStream(part.getInputStream())) {
			return toHex(digest(in));
		}
	}

	private String toHex(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		return IntStream.generate(buffer::get).limit(buffer.remaining()).mapToObj(i -> String.format("%02x", (byte) i))
				.collect(Collectors.joining());
	}

	private byte[] digest(InputStream in) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] buffer = new byte[256];
			int length = -1;
			while ((length = in.read(buffer)) > -1) {
				md.update(buffer, 0, length);
			}
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}