/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.detect.interest.UnrollSiftScaleSpaceGradient;
import boofcv.core.image.GConvertImage;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Allows you to use SIFT features independent of the SIFT detector.  A SIFT scale-space is computed with all octaves
 * and most of the scales saved.  When a few feature is requested it looks up the closest scale image and uses
 * that as the input image.
 *
 * @author Peter Abeles
 */
public class DescribeRegionPoint_SIFT <T extends ImageSingleBand>
	implements DescribeRegionPoint<T,TupleDesc_F64>
{

	ImageType<T> imageType;

	UnrollSiftScaleSpaceGradient scaleSpace;
	DescribePointSift<ImageFloat32> describe;

	ImageFloat32 imageFloat = new ImageFloat32(1,1);

	public DescribeRegionPoint_SIFT(SiftScaleSpace scaleSpace,
									DescribePointSift<ImageFloat32> describe,
									Class<T> imageType ) {
		this.scaleSpace = new UnrollSiftScaleSpaceGradient(scaleSpace);
		this.describe = describe;

		this.imageType = ImageType.single(imageType);
	}

	@Override
	public void setImage(T image) {
		ImageFloat32 input;
		if( image instanceof ImageFloat32 ) {
			input = (ImageFloat32)image;
		} else {
			imageFloat.reshape(image.width,image.height);
			GConvertImage.convert(image,imageFloat);
			input = imageFloat;
		}

		scaleSpace.setImage(input);
	}

	@Override
	public boolean process(double x, double y, double orientation, double radius, TupleDesc_F64 description) {

		System.out.println("orientation = "+orientation);
		// get the blur sigma for the radius
		double sigma = radius/ BoofDefaults.SIFT2_SCALE_TO_RADIUS;

		// find the image which the blur factor closest to this sigma
		UnrollSiftScaleSpaceGradient.ImageScale image = scaleSpace.lookup(sigma);

		// compute the descriptor
		describe.setImageGradient(image.derivX,image.derivY);
		describe.process(x/image.imageToInput,y/image.imageToInput,sigma/image.imageToInput,
				orientation,description);

		return true;
	}

	@Override
	public boolean requiresRadius() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public double getCanonicalWidth() {
		return describe.getCanonicalRadius()*2;
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(describe.getDescriptorLength());
	}

	@Override
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}
}
