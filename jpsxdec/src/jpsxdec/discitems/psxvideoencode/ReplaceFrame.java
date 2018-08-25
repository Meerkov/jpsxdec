/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.discitems.psxvideoencode;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.savers.FrameLookup;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.i18n.I;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.Ac0Cleaner;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.IO;
import jpsxdec.util.LocalizedIncompatibleException;
import jpsxdec.util.LoggedFailure;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReplaceFrame { // rename to ReplaceFrameFull

	private static final Logger LOG = Logger.getLogger(ReplaceFrame.class.getName());

	@Nonnull
	private final FrameLookup _frameNum;
	@Nonnull
	private final File _imageFile;
	@CheckForNull
	private ImageFormat _format;

	private Boolean _allowAlphaBlending;
	private Boolean _denoise;
	public static final String XML_TAG_NAME = "replace";

	public enum ImageFormat {
		BS, MDEC;
		public static @CheckForNull ImageFormat deserialize(@CheckForNull String sFormat) throws DeserializationFail {
			if (sFormat == null || sFormat.length() == 0)
				return null;
			else if ("bs".equalsIgnoreCase(sFormat))
				return BS;
			else if ("mdec".equalsIgnoreCase(sFormat))
				return MDEC;
			else
				throw new DeserializationFail(I.REPLACE_INVALID_IMAGE_FORMAT(sFormat));
		}

		public @Nonnull String serialize() {
			return name().toLowerCase();
		}
	}

	public ReplaceFrame(@Nonnull Element element) throws DeserializationFail {
		this(element.getAttribute("frame").trim(), element.getFirstChild().getNodeValue().trim());
		setFormat(ImageFormat.deserialize(element.getAttribute("format")));
		_allowAlphaBlending = Boolean.valueOf(element.getAttribute("allow-alpha-blending"));
		_denoise = Boolean.valueOf(element.getAttribute("denoise"));
	}

	public @Nonnull Element serialize(@Nonnull Document document) {
		Element node = document.createElement(XML_TAG_NAME);
		node.setAttribute("frame", getFrameLookup().toString());
		node.setTextContent(getImageFile().toString());
		ImageFormat fmt = getFormat();
		if (fmt != null)
			node.setAttribute("format", fmt.serialize());
		return node;
	}

	public ReplaceFrame(@Nonnull String sFrameNumber, @Nonnull String sImageFile) throws DeserializationFail {
		this(sFrameNumber, new File(sImageFile));
	}

	public ReplaceFrame(@Nonnull String sFrameNumber, @Nonnull File imageFile) throws DeserializationFail {
		this(FrameLookup.deserialize(sFrameNumber), imageFile);
	}

	public ReplaceFrame(@Nonnull FrameLookup frameNumber, @Nonnull String sImageFile) {
		this(frameNumber, new File(sImageFile));
	}

	public ReplaceFrame(@Nonnull FrameLookup frameNumber, @Nonnull File imageFile) {
		_frameNum = frameNumber;
		_imageFile = imageFile;
	}

	final public @Nonnull FrameLookup getFrameLookup() {
		return _frameNum;
	}

	final public @Nonnull File getImageFile() {
		return _imageFile;
	}

	final public @CheckForNull ImageFormat getFormat() {
		return _format;
	}

	final public void setFormat(@CheckForNull ImageFormat format) {
		_format = format;
	}

	public void replace(@Nonnull IDemuxedFrame frame, @Nonnull CdFileSectorReader cd, @Nonnull ILocalizedLogger log)
			throws LoggedFailure {
		// identify existing frame bs format
		byte[] abExistingFrame = frame.copyDemuxData(null);
		BitStreamUncompressor bsu;
		try {
			bsu = BitStreamUncompressor.identifyUncompressor(abExistingFrame);
		} catch (BinaryDataNotRecognized ex) {
			throw new LoggedFailure(log, Level.SEVERE,
					I.UNABLE_TO_DETERMINE_FRAME_TYPE_FRM(getFrameLookup().toString()), ex);
		}

		byte[] abNewFrame;

		if (_format == ImageFormat.BS) {
			abNewFrame = readBitstream(_imageFile, log);
		} else {
			BitStreamCompressor compressor = bsu.makeCompressor();
			if (_format == ImageFormat.MDEC) {
				abNewFrame = readMdec(_imageFile, getFrameLookup(), frame.getWidth(), frame.getHeight(), compressor,
						log);
			} else {
				abNewFrame = readJavaImage(_imageFile, getFrameLookup(), frame.getWidth(), frame.getHeight(),
						abExistingFrame, compressor, bsu, log, getFrameLookup(), _allowAlphaBlending, _denoise);
			}
		}

		if (abNewFrame == null)
			throw new LoggedFailure(log, Level.SEVERE,
					I.CMD_UNABLE_TO_COMPRESS_FRAME_SMALL_ENOUGH(getFrameLookup().toString(), frame.getDemuxSize()));
		else if (abNewFrame.length > frame.getDemuxSize()) // for bs or mdec
															// formats
			throw new LoggedFailure(log, Level.SEVERE,
					I.NEW_FRAME_DOES_NOT_FIT(getFrameLookup().toString(), abNewFrame.length, frame.getDemuxSize()));

		try {
			// find out how many bytes and mdec codes are used by the new frame
			bsu.reset(abNewFrame);
		} catch (BinaryDataNotRecognized ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_BITSTREAM_MISMATCH(_imageFile), ex);
		}

		try {
			bsu.skipMacroBlocks(frame.getWidth(), frame.getHeight());
			bsu.skipPaddingBits();
		} catch (MdecException.EndOfStream ex) {
			throw new RuntimeException("Can't decode a frame we just encoded?", ex);
		} catch (MdecException.ReadCorruption ex) {
			throw new RuntimeException("Can't decode a frame we just encoded?", ex);
		}

		int iUsedSize = ((bsu.getBitPosition() + 15) / 16) * 2; // rounded up to
																// nearest word
		frame.writeToSectors(abNewFrame, iUsedSize, bsu.getMdecCodeCount(), cd, log);
	}

	private static byte[] readBitstream(@Nonnull File imageFile, @Nonnull ILocalizedLogger log) throws LoggedFailure {
		try {
			return IO.readFile(imageFile);
		} catch (FileNotFoundException ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_NOT_FOUND_NAME(imageFile.toString()), ex);
		} catch (IOException ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(imageFile.toString()), ex);
		}
	}

	private static byte[] readMdec(@Nonnull File imageFile, @Nonnull FrameLookup frameNum, int iWidth, int iHeight,
			@Nonnull BitStreamCompressor compressor, @Nonnull ILocalizedLogger log) throws LoggedFailure {
		try {
			MdecInputStreamReader mdecIn = new MdecInputStreamReader(IO.readFile(imageFile));
			return compressor.compress(mdecIn, iWidth, iHeight);
		} catch (FileNotFoundException ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_NOT_FOUND_NAME(imageFile.toString()), ex);
		} catch (IOException ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(imageFile.toString()), ex);
		} catch (IncompatibleException ex) {
			throw new LoggedFailure(log, Level.SEVERE,
					I.REPLACE_INCOMPATIBLE_MDEC(imageFile.toString(), frameNum.toString()), ex);
		} catch (MdecException.TooMuchEnergy ex) {
			throw new LoggedFailure(log, Level.SEVERE,
					I.REPLACE_INCOMPATIBLE_MDEC(imageFile.toString(), frameNum.toString()), ex);
		} catch (MdecException.EndOfStream ex) {
			throw new LoggedFailure(log, Level.SEVERE,
					I.REPLACE_INCOMPLETE_MDEC(imageFile.toString(), frameNum.toString()), ex);
		} catch (MdecException.ReadCorruption ex) {
			throw new LoggedFailure(log, Level.SEVERE,
					I.REPLACE_CORRUPTED_MDEC(imageFile.toString(), frameNum.toString()), ex);
		}
	}

	private static byte[] readJavaImage(@Nonnull File imageFile, @Nonnull FrameLookup frameNum, int iWidth, int iHeight,
			byte[] abExistingFrame, @Nonnull BitStreamCompressor compressor, BitStreamUncompressor bsu,
			@Nonnull ILocalizedLogger log, FrameLookup frameLookup, boolean allowAlphaBlending, boolean allowDenoise) throws LoggedFailure {
		BufferedImage bi;
		try {
			bi = ImageIO.read(imageFile);
		} catch (IOException ex) {
			throw new LoggedFailure(log, Level.SEVERE, I.IO_READING_FILE_ERROR_NAME(imageFile.toString()), ex);
		}
		if (bi == null)
			throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_FILE_NOT_JAVA_IMAGE(imageFile));

		if (bi.getWidth() != Calc.fullDimension(iWidth) || bi.getHeight() != Calc.fullDimension(iHeight))
			throw new LoggedFailure(log, Level.SEVERE, I.REPLACE_FRAME_DIMENSIONS_MISMATCH(imageFile.toString(),
					bi.getWidth(), bi.getHeight(), iWidth, iHeight));

		if (allowAlphaBlending) {

			final int WIDTH = bi.getWidth();
			final int HEIGHT = bi.getHeight();
			MdecDecoder_double decoder = new MdecDecoder_double(new StephensIDCT(), WIDTH, HEIGHT);
			try {
				ParsedMdecImage parsedOrig = new ParsedMdecImage(new Ac0Cleaner(bsu), WIDTH, HEIGHT);

				// TODO: use best quality to decode, but same as encode
				decoder.decode(parsedOrig.getStream());
				RgbIntImage rgb = new RgbIntImage(bi.getWidth(), bi.getHeight());
				decoder.readDecodedRgb(rgb.getWidth(), rgb.getHeight(), rgb.getData());
				BufferedImage origImg = rgb.toBufferedImage();

				final BufferedImage alphaImage = new BufferedImage(origImg.getWidth(), origImg.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				final Graphics g = alphaImage.getGraphics();
				g.drawImage(origImg, 0, 0, null);
				g.drawImage(bi, 0, 0, null);
				g.dispose();
				bi = alphaImage;
			} catch (MdecException.EndOfStream ex) {
				// existing frame is incomplete
				throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_INCOMPLETE(frameLookup.toString()), ex);
			} catch (MdecException.ReadCorruption ex) {
				// existing frame is corrupted
				throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_CORRUPTED(frameLookup.toString()), ex);
			}
		}

		PsxYCbCrImage psxImage = new PsxYCbCrImage(bi);
		if (allowDenoise) {
			psxImage.simpleDenoise();
		}

		MdecEncoder encoder = new MdecEncoder(psxImage, iWidth, iHeight);
		try {
			return compressor.compressFull(abExistingFrame, frameNum.toString(), encoder, log);
		} catch (MdecException.EndOfStream ex) {
			// existing frame is incomplete
			throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_INCOMPLETE(frameNum.toString()), ex);
		} catch (MdecException.ReadCorruption ex) {
			// existing frame is corrupted
			throw new LoggedFailure(log, Level.SEVERE, I.FRAME_NUM_CORRUPTED(frameNum.toString()), ex);
		}
	}
}
