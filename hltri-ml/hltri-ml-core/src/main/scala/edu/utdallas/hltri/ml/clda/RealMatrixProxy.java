package edu.utdallas.hltri.ml.clda;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Created by travis on 2/2/15.
 */
public class RealMatrixProxy extends AbstractRealMatrix {
  final double w_d_n_i[][][];
  final int R;
  final int C;

  int D;
  int N_d[];

  /**
   * Creates a matrix with no data
   */
  public RealMatrixProxy(double[][][] w_d_n_i, int r, int c, int D, int N_d[]) {
    this.w_d_n_i = w_d_n_i;
    R = r;
    C = c;
    this.D = D;
    this.N_d = N_d;
  }

  /**
   * Returns the number of rows of this matrix.
   *
   * @return the number of rows.
   */
  @Override public int getRowDimension() {
    return R;
  }

  /**
   * Returns the number of columns of this matrix.
   *
   * @return the number of columns.
   */
  @Override public int getColumnDimension() {
    return C;
  }

  /**
   * {@inheritDoc}
   *
   * @param rowDimension
   * @param columnDimension
   */
  @Override public RealMatrix createMatrix(int rowDimension, int columnDimension) throws NotStrictlyPositiveException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override public RealMatrix copy() {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param row
   * @param column
   */
  @Override public double getEntry(int row, int column) throws OutOfRangeException {
    for (int d = 0; d < D; d++) {
      if (row < N_d[d]) {
        return w_d_n_i[d][row][column];
      } else {
        row -= N_d[d];
      }
    }
    throw new RuntimeException("Failed to get entry (" + row + ", " + column + ")!");
  }

  /**
   * {@inheritDoc}
   *
   * @param row
   * @param column
   * @param value
   */
  @Override public void setEntry(int row, int column, double value) throws OutOfRangeException {
    throw new UnsupportedOperationException();
  }
}
