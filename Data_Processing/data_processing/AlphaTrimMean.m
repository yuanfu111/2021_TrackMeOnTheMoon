function Y = AlphaTrimMean(A,w,alpha)
%ALPHATRIMMEAN Filter the input signal stream in the vector A with
%window w and alpha factor.
%   A must be a vector, w must be odd value, 0<alpha<0.5
    
    if isvector(A)
        if mod(w,2)==1
            if alpha>0 && alpha<0.5
                if w<=length(A)
                    
                    AA = zeros(size(A));
                    ww = (w-1)/2;
                    for i=1:length(A)
                        s = max(1,i-ww);
                        e = min(i+ww,length(A));
                        window = A(s:e);
                        window = sort(window);
                        t = floor(length(window)*alpha);
                        window = window(t+1:end-t);
                        AA(i) = mean(window);
                    end
                    Y = AA;
                    
                else
                    error('The window size must be greater than or equal the length of the vector')
                end
            else
                error('Alpha must range from 0 to 0.5')
            end
        else
            error('The window size must be odd number')
        end
    else
        error('The input must be a vector, this function for vectors only')
    end
end